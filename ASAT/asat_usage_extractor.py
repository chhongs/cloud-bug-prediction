from pathlib import Path
import subprocess
import re

from model.asat_usage import ASATUsage
from model.arg_usage import ArgUsage


class ASATUsageExtractor:
    """Extract ASAT usages from a repository."""

    def __init__(self, asats):
        self.asats = asats

    def extract(self, repo_path):
        """Extract the ASATs used in the given repository."""
        asat_usages = []

        for asat in self.asats:
            print(asat.name)
            asat_usage = self.get_asat_usage(repo_path, asat)
            if asat_usage:
                asat_usages.append(asat_usage)

        return asat_usages

    @classmethod
    def get_asat_usage(cls, repo_path, asat):
        """Create an ASATUsage instance if ASAT is used in the repository."""
        asat_cmd_usages = cls.get_cmd_usages(repo_path, asat)
        if asat_cmd_usages:
            asat_usage = ASATUsage(asat=asat.name)

            for cmd_usage in asat_cmd_usages:
                filepath, cmd_statement = cmd_usage.split(':', maxsplit=1)
                # ignore comments
                if cls.is_cmd_statement(filepath, cmd_statement):
                    asat_usage.files.add(Path(filepath).name)
                    arg_usage = cls.get_arg_usage(cmd_statement, asat.command)
                    asat_usage.arg_usage.update(arg_usage)

            return asat_usage

        return None

    @classmethod
    def is_cmd_statement(cls, filepath, cmd_statement):
        not_install = 'go get' not in cmd_statement
        not_print = 'echo' not in cmd_statement
        not_comment = not cls.is_comment(cmd_statement)
        source_code = cls.is_source_code(filepath)
        return not_comment and source_code and not_print and not_install

    @staticmethod
    def is_comment(cmd_statement):
        markers = ['//', '#']
        for marker in markers:
            if marker in cmd_statement:
                return True
        return False

    @staticmethod
    def is_source_code(file_path):
        exts = ['.md', '.txt', '.html']
        for ext in exts:
            if file_path.endswith(ext):
                return False
        return True

    @staticmethod
    def get_cmd_usages(repo_path, asat):
        """Get command usages of a specific ASAT in the repository's files."""
        cmd_usages = []
        proc = subprocess.run(
            f'grep -r "[^A-Za-z]{asat.command}[^A-Za-z]" {repo_path}',
            shell=True,
            stdout=subprocess.PIPE,
            encoding='utf-8')
        if proc.stdout:
            for cmd_usage in proc.stdout.split('\n'):
                # ignore lines containing no matches
                if ':' in cmd_usage:
                    cmd_usages.append(cmd_usage)

        return cmd_usages

    @staticmethod
    def get_arg_usage(cmd_statement, asat_cmd):
        """Get argument usage in command statement."""
        cmd_statement = cmd_statement.strip().strip('"')
        arg_usage = ArgUsage()

        match = re.search(rf'{asat_cmd}((\s+(.*))|$)', cmd_statement)
        if match and match.group(1):
            argline = match.group(1)
            arg_usage.raw = argline

            # add positionals
            positionals_line = argline.split('-', maxsplit=1)[0].strip()
            if positionals_line:
                arg_usage.positionals = re.split(r'\s+', positionals_line)

            # add options/named arguments
            for arg in re.finditer(r'--?\S+[^-]*', argline):
                parts = re.split(r'=|\s+', arg.group().strip())
                name = parts[0]
                if len(parts) == 2:
                    value = parts[1].strip('"')
                    arg_usage.named[name].append(value)
                else:
                    arg_usage.options.add(name)

        return arg_usage

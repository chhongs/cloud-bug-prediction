from typing import List
from pathlib import Path
import glob
import subprocess
import re

from model.asat_usage import ASATUsage
from model.arg_usage import ArgUsage
from model.asat import ASAT
from parsers.golangci_parser import GolangCIParser


class ASATUsageExtractor:
    """Extract ASAT usages from a repository."""

    def __init__(self, asats: List[ASAT]):
        self.asats = asats

    def extract(self, repo_path):
        """Extract the ASATs used in the given repository."""
        asat_usages = {}

        for asat in self.asats:
            asat_usage = self.get_asat_usage(repo_path, asat)
            if asat_usage:
                asat_usages[asat.name] = asat_usage

        for asat_usage in self.get_golangci_asat_usages(repo_path):
            if asat_usage.asat.name not in asat_usages:
                asat_usages[asat_usage.asat.name] = asat_usage

        return list(asat_usages.values())

    def get_golangci_asat_usages(self, repo_path: str) -> List[ASATUsage]:
        """Get the ASAT usages from the golangci configuration file."""
        asat_usages = []
        asats = {asat.name: asat for asat in self.asats}
        pattern = Path(repo_path) / '.golangci.*'
        for path in glob.glob(str(pattern), recursive=True):
            parser = GolangCIParser(path)
            golangci = parser.parse()
            for asat_name in golangci.enabled:
                if asat_name in asats:
                    asat = asats[asat_name]
                    asat_usage = ASATUsage(asat)
                    asat_usage.files.add(Path(path).name)
                    asat_usages.append(asat_usage)

        return asat_usages

    @classmethod
    def get_asat_usage(cls, repo_path, asat):
        """Create an ASATUsage instance if ASAT is used in the repository."""
        asat_cmd_usages = cls.get_cmd_usages(repo_path, asat)

        asat_usage = None

        for cmd_usage in asat_cmd_usages:
            filepath, cmd_statement = cmd_usage.split(':', maxsplit=1)
            # ignore comments
            if cls.is_cmd_statement(filepath, cmd_statement, asat.command):
                if not asat_usage:
                    asat_usage = ASATUsage(asat=asat)
                asat_usage.files.add(Path(filepath).name)
                arg_usage = cls.get_arg_usage(cmd_statement, asat.command)
                asat_usage.arg_usage.update(arg_usage)

        return asat_usage

    @classmethod
    def is_cmd_statement(cls, filepath, cmd_statement, asat):
        not_golangci_file = 'golangci' not in filepath
        not_string = cls.not_string(asat, cmd_statement)
        not_install = cls.not_install(cmd_statement)
        not_print = 'echo' not in cmd_statement
        not_comment = not cls.is_comment(cmd_statement)
        source_code = cls.is_source_code(filepath)
        return not_comment \
            and source_code \
            and not_print \
            and not_install \
            and not_string \
            and not_golangci_file

    @staticmethod
    def not_install(cmd_statement):
        """Ignore ASAT command installation commands."""
        return 'go get' not in cmd_statement \
            and 'honnef.co' not in cmd_statement

    @staticmethod
    def not_string(asat_cmd, cmd_statement):
        """Check if ASAT command is used within a string.

        Some ASAT commands represent common english words and therefore cause
        lots of false positives (e.g. "whitespace" or "unused").
        """
        blacklist = {'whitespace', 'unused', 'misspell'}
        if asat_cmd in blacklist:
            if re.search(rf'"[^"]*{asat_cmd}[^"]*"', cmd_statement):
                return False
        return True

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
            f'grep -r "{asat.command}" {repo_path}',
            shell=True,
            stdout=subprocess.PIPE,
            encoding='utf-8')
        if proc.stdout:
            for cmd_usage in proc.stdout.split('\n'):
                # ignore lines containing no matches
                if ':' in cmd_usage:
                    # do addtional filtering here
                    # since extended grep command is slow
                    re_str = rf'\b{asat.command}([^0-9A-Za-z_:\-]|$)'
                    if re.search(re_str, cmd_usage):
                        cmd_usages.append(cmd_usage)

        return cmd_usages

    @classmethod
    def get_arg_usage(cls, cmd_statement, asat_cmd):
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
                name = cls.remove_non_cmd_elements(parts[0])
                if len(parts) == 2:
                    value = cls.remove_non_cmd_elements(parts[1])
                    arg_usage.named[name].append(value)
                else:
                    arg_usage.options.add(name)

        return arg_usage

    @staticmethod
    def remove_non_cmd_elements(param):
        return re.sub(r'(.*?)[\])\',"]+$', r'\1', param)

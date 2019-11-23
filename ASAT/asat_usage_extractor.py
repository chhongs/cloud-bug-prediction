from pathlib import Path
import subprocess
import re

from model.asat_usage import ASATUsage


class ASATUsageExtractor:
    """Extract ASAT usages from a repository."""

    def __init__(self, asats):
        self.asats = asats

    def extract(self, repo_path):
        """Extract the ASATs used in the given repository."""
        asat_usages = []

        for asat in self.asats:
            asat_usage = self.get_asat_usage(repo_path, asat)
            if asat_usage:
                asat_usages.append(asat_usage)

        return asat_usages

    @classmethod
    def get_asat_usage(cls, repo_path, asat):
        """Create an ASATUsage instance if ASAT is used in the repository."""
        asat_cmd_usages = cls.get_cmd_usages(repo_path, asat)
        if asat_cmd_usages:
            asat_name = asat.name
            cfg_path = None
            files = set()
            args = set()

            for cmd_usage in asat_cmd_usages:
                filepath, cmd_statement = cmd_usage.split(':', maxsplit=1)
                files.add(Path(filepath).name)
                args.update(cls.get_args(cmd_statement, asat.command))

            return ASATUsage(
                asat=asat_name, cfg_path=cfg_path, files=files, args=args)

        return None

    @staticmethod
    def get_cmd_usages(repo_path, asat):
        """Get command usages of a specific ASAT in the repository's files."""
        cmd_usages = []
        proc = subprocess.run(
            f'grep -r "{asat.command}[^A-Za-z]" {repo_path}',
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
    def get_args(cmd_statement, asat_cmd):
        """Get arguments in an command statement."""
        args = set()
        argline = re.search(rf'{asat_cmd}(.*)', cmd_statement).group(1)
        for arg in re.finditer(r'--?\S+', argline):
            args.add(arg.group())

        return args

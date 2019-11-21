from pathlib import Path
import subprocess
import re

from model.asat_usage import ASATUsage


class ASATUsageExtractor:
    """Extract ASAT usages from repositories."""

    def __init__(self, asats):
        self.asats = asats

    def extract(self, repo_path):
        asat_usages = []

        for asat in self.asats:
            asat_cmd_usages = self.get_cmd_usages(repo_path, asat)
            if asat_cmd_usages:
                asat_name = asat.name
                cfg_path = None
                files = set()
                args = set()

                for cmd_usage in asat_cmd_usages:
                    filepath, cmd = cmd_usage.split(':', maxsplit=1)
                    files.add(Path(filepath).name)

                    argline = re.search(rf'{asat.command}(.*)', cmd).group(1)
                    for arg in re.finditer(r'--?\S+', argline):
                        args.add(arg.group())

                asat_usage = ASATUsage(
                    asat=asat_name, cfg_path=cfg_path, files=files, args=args)
                asat_usages.append(asat_usage)

        return asat_usages

    @staticmethod
    def get_cmd_usages(repo_path, asat):
        """Get command usages for a specific ASAT in the repository's files."""
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

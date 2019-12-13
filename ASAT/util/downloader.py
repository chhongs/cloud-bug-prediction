import subprocess
from pathlib import Path


class RepoDownloader:
    """Class for downloading repositories to have them locally."""

    def __init__(self, repos_path):
        self.repos_path = Path(repos_path)

    def download(self, url):
        repo_dirname = self.get_repo_dirname(url)
        repo_path = self.repos_path / repo_dirname
        if not self.repo_exists_locally(repo_path):
            subprocess.run(
                f'git clone {url} {repo_path}',
                shell=True,
                encoding='utf-8')

        return repo_path

    @staticmethod
    def get_repo_dirname(url):
        return Path(url).name

    @staticmethod
    def repo_exists_locally(repo_path):
        return Path(repo_path).is_dir()

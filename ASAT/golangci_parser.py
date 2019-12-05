import yaml
from model.golangci import GolangCI


class GolangCIParser:

    def __init__(self, path: str):
        self.yaml = self._get_yaml(path)
        self.golangci = None

    @staticmethod
    def _get_yaml(path: str):
        with open(path) as f:
            return yaml.load(f)

    def set_linters(self):
        disable_all = self.yaml['linters']['disable_all']
        enabled = self.yaml['linters']['enable']
        disabled = self.yaml['linters']['disable']

        if disable_all:
            for linter in self.golangci.enabled:
                self.golangci.disabled.add(linter)
            self.golangci.enabled = set()
            for linter in enabled:
                self.golangci.enabled.add(linter)
        else:
            for linter in enabled:
                self.golangci.enabled.add(linter)
                self.golangci.disabled.remove(linter)

            for linter in disabled:
                self.golangci.enabled.remove(linter)
                self.golangci.disabled.add(linter)

    def parse(self) -> GolangCI:
        self.golangci = GolangCI()
        self.set_linters()
        return self.golangci

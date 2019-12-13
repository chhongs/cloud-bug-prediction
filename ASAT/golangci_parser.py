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
        disable_all = self._disable_all()
        enabled = self.yaml['linters'].get('enable', [])
        disabled = self.yaml['linters'].get('disable', [])

        if disable_all:
            for linter in self.golangci.enabled:
                self.golangci.disabled.add(linter)
            self.golangci.enabled = set()
            for linter in enabled:
                self.golangci.enabled.add(linter)
        else:
            for linter in enabled:
                self.golangci.enabled.add(linter)
                self.golangci.disabled.discard(linter)

            for linter in disabled:
                self.golangci.enabled.discard(linter)
                self.golangci.disabled.add(linter)

    def _disable_all(self):
        if 'disable_all' in self.yaml['linters']:
            disable_all_str = self.yaml['linters']['disable_all']
            if disable_all_str == 'true':
                disable_all = True
            else:
                disable_all = False
        else:
            disable_all = False
        return disable_all

    def parse(self) -> GolangCI:
        self.golangci = GolangCI()
        self.set_linters()
        return self.golangci

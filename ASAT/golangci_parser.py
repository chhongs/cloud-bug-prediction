import yaml
import toml
from model.golangci import GolangCI


class GolangCIParser:

    def __init__(self, path: str):
        if path.endswith('yml'):
            self.config = self._get_yaml(path)
        elif path.endswith('toml'):
            self.config = self._get_toml(path)

        self.golangci = None

    @staticmethod
    def _get_yaml(path: str):
        with open(path) as f:
            return yaml.load(f)

    @staticmethod
    def _get_toml(path: str):
        return toml.load(path)

    def set_linters(self):
        disable_all = self._disable_all()
        enabled = self.config['linters'].get('enable', [])
        disabled = self.config['linters'].get('disable', [])

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
        if 'disable_all' in self.config['linters']:
            disable_all_str = self.config['linters']['disable_all']
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

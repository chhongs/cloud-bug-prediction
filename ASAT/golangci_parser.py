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
            return yaml.full_load(f)

    @staticmethod
    def _get_toml(path: str):
        return toml.load(path)

    def set_linters(self):
        disable_all = self._get_linters_param_val('disable-all')
        enable_all = self._get_linters_param_val('enable-all')
        enabled = self.config['linters'].get('enable', [])
        disabled = self.config['linters'].get('disable', [])

        if disable_all:
            for linter in self.golangci.enabled:
                self.golangci.disabled.add(linter)
            self.golangci.enabled = set()

        if enable_all:
            for linter in self.golangci.disabled:
                self.golangci.enabled.add(linter)
            self.golangci.disabled = set()

        for linter in enabled:
            self.golangci.enabled.add(linter)
            self.golangci.disabled.discard(linter)

        for linter in disabled:
            self.golangci.enabled.discard(linter)
            self.golangci.disabled.add(linter)

    def _get_linters_param_val(self, linters_param):
        if linters_param in self.config['linters']:
            val_str = self.config['linters'][linters_param]
            if val_str == 'true':
                val_bool = True
            else:
                val_bool = False
        else:
            val_bool = False
        return val_bool

    def parse(self) -> GolangCI:
        self.golangci = GolangCI()
        self.set_linters()
        return self.golangci

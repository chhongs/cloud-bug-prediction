from typing import Set


class GolangCI:
    enabled: Set[str]
    disabled: Set[str]

    def __init__(self, ):
        self.initialized_default_linters()

    def initialized_default_linters(self):
        self.enabled = {
            'deadcode',
            'errcheck',
            'gosimple',
            'govet',
            'ineffassign',
            'staticcheck',
            'structcheck',
            'typecheck',
            'unused',
            'varcheck',
        }

        self.disabled = {
            'bodyclose',
            'depguard',
            'dogsled',
            'dupl',
            'funlen',
            'gochecknoglobals',
            'gochecknoinits',
            'gocognit',
            'goconst',
            'gocritic',
            'gocyclo',
            'godox',
            'gofmt',
            'goimports',
            'golint',
            'gomnd',
            'gosec',
            'interfacer',
            'lll',
            'maligned',
            'misspell',
            'nakedret',
            'prealloc',
            'scopelint',
            'stylecheck',
            'unconvert',
            'unparam',
            'whitespace',
            'wsl',
        }

from dataclasses import dataclass, field
from typing import List, Dict, Set
from collections import defaultdict


@dataclass
class ArgUsage:
    raw: str = ''
    positionals: List[str] = field(default_factory=list)
    options: Set[str] = field(default_factory=set)
    named: Dict[str, List[str]] = field(
        default_factory=lambda: defaultdict(list))

    def update(self, arg_usage):
        """Add arguments from an ArgUsage instance.

        Args:
            arg_usage (ArgUsage): The ArgUsage instance.
        """
        self.positionals.extend(arg_usage.positionals)
        self.options.update(arg_usage.options)
        for k, v in arg_usage.named.items():
            self.named[k].extend(v)

    def get_parameters(self):
        return self.options | set(self.named.keys())

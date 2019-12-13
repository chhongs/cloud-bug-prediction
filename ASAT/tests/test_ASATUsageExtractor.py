from unittest import TestCase
from parsers.asat_usage_extractor import ASATUsageExtractor as extr
from model.arg_usage import ArgUsage


class TestASATUsageExtractor(TestCase):

    def test_get_args(self):
        actual_arg_usage = extr.get_arg_usage(
            'cmd arg1 -o arg2 -p --xxx arg3 --error', 'cmd')

        expected_arg_usage = ArgUsage(
            raw=' arg1 -o arg2 -p --xxx arg3 --error',
            options={'-p', '--error'},
            positionals=['arg1'],
            named={'-o': ['arg2'], '--xxx': ['arg3']}
        )

        self.assertEqual(actual_arg_usage, expected_arg_usage)


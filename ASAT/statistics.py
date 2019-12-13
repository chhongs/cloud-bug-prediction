from typing import List
import matplotlib.pyplot as plt
from model.project import Project


def get_asat_usage_numbers(projects: List[Project]):
    """Get number of projects per ASAT."""
    numbers = {}
    for project in projects:
        for asat_usage in project.asat_usages:
            asat = asat_usage.asat
            if asat not in numbers:
                numbers[asat] = 0
            numbers[asat] += 1

    return numbers


def plot_asat_usage_percentages(asats, projects):
    numbers = get_asat_usage_numbers(projects)
    n_projects = len(projects)

    for asat in asats:
        if asat.name not in numbers:
            numbers[asat.name] = 0

    group_data = [(num / n_projects)*100 for num in numbers.values()]
    group_names = list(numbers.keys())

    fig, ax = plt.subplots(figsize=(15, 15))
    ax.barh(group_names, group_data)
    ax.set(xlabel='Percentage',
           ylabel='ASAT',
           title='Percentages for ASATs used in projects')
    plt.show()


def print_average_number_of_asats(projects):
    total = 0
    for project in projects:
        total += len(project.asat_usages)

    print('Average number of ASATs used by projects', total/len(projects))


def print_asat_arg_usage(projects: List[Project]):
    param_numbers = {}
    asat_numbers = {}
    for project in projects:
        for asat_usage in project.asat_usages:
            asat = asat_usage.asat
            if asat not in asat_numbers:
                asat_numbers[asat] = 0
            asat_numbers[asat] += 1

            if asat not in param_numbers:
                param_numbers[asat] = {}

            arg_usage = asat_usage.arg_usage
            for param in arg_usage.get_parameters():
                if param not in param_numbers[asat]:
                    param_numbers[asat][param] = 0

                param_numbers[asat][param] += 1

    for asat in param_numbers:
        print('ASAT: ', asat)
        asat_num = asat_numbers[asat]
        for param in param_numbers[asat]:
            param_num = int(param_numbers[asat][param])
            print(f'\tParameter: {param}, {param_num}/{asat_num}')

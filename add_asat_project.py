import subprocess
import re
import pathlib
import sqlite3

conn = sqlite3.connect('projects.sqlite3')
c = conn.cursor()
res = c.execute("SELECT Name, Command FROM ASATs")
ASATs = res.fetchall()

project_path = '../projects/nats-server'
project_url = 'https://github.com/nats-io/nats-server'

tools = {}

for row in ASATs:
    ASAT_name, ASAT_cmd = row

    proc = subprocess.run(
        f'grep -r "{ASAT_cmd}[^A-Za-z]" {project_path}',
        shell=True,
        stdout=subprocess.PIPE,
        encoding='utf-8')

    if proc.stdout:
        print(proc.stdout)
        if ASAT_name not in tools:
            tools[ASAT_name] = (set(), set())

            matches = proc.stdout.split('\n')
            for match in matches:
                if ':' in match:
                    p, line = match.split(':', maxsplit=1)
                    path = pathlib.Path(p)
                    tools[ASAT_name][0].add(path.name)

                    argline = re.search(rf'{ASAT_cmd}(.*)', line).group(1)
                    for arg in re.finditer(r'--?\S+', argline):
                        tools[ASAT_name][1].add(arg.group())


for ASAT_name in tools:
    filenames = ','.join(tools[ASAT_name][0])
    args = ','.join(tools[ASAT_name][1])

    c.execute(
        'INSERT OR REPLACE INTO '
        'project_ASAT (Project, ASAT, `Config Path`, Files, Args) '
        'VALUES(?,?,?,?,?)',
        [project_url, ASAT_name, None, filenames, args])

conn.commit()
conn.close()

import sqlite3

from model.asat import ASAT
from model.project import Project


class DB:
    """Class for fetching/inserting data about ASAT usage in projects."""

    def __init__(self, db_path='projects.sqlite3'):
        with sqlite3.connect(db_path) as conn:
            self.cursor = conn.cursor()

    def get_projects(self):
        projects = []
        res = self.cursor.execute("SELECT * FROM Projects")
        for row in res.fetchall():
            url, desc, stars, commits, is_cloud_app = row
            project = Project(url=url,
                              description=desc,
                              stars=stars,
                              commits=commits,
                              asat_usages=[],
                              is_cloud_app=bool(is_cloud_app)
            )
            projects.append(project)

        return projects

    def get_ASATs(self):
        asats = []
        res = self.cursor.execute("SELECT * FROM ASATs")
        for row in res.fetchall():
            name, desc, docs, cfgs, cmd, aggregator, category = row
            asat = ASAT(name=name,
                        description=desc,
                        docs=docs,
                        configs=cfgs.split(', ') if cfgs else [],
                        command=cmd,
                        aggregator=bool(aggregator),
                        category=category)
            asats.append(asat)

        return asats

    # def insert_asat_usages(self, project):
    #     for asat_usage in project.asat_usages:
    #         self.cursor.execute(
    #             'INSERT OR REPLACE INTO '
    #             'project_ASAT (Project, ASAT, `Config Path`, Files, Args) '
    #             'VALUES(?,?,?,?,?)',
    #             [project.url,project , None, filenames, args])

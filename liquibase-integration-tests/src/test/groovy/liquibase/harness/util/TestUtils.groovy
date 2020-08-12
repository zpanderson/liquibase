package liquibase.harness.util

import liquibase.CatalogAndSchema
import liquibase.Liquibase
import liquibase.change.Change
import liquibase.changelog.ChangeSet
import liquibase.database.Database
import liquibase.harness.config.DatabaseUnderTest
import liquibase.harness.config.DatabaseVersion
import liquibase.harness.config.TestConfig
import liquibase.harness.config.TestInput
import liquibase.resource.ResourceAccessor
import liquibase.sql.Sql
import liquibase.sqlgenerator.SqlGeneratorFactory
import liquibase.test.JUnitResourceAccessor

class TestUtils {

    static Liquibase createLiquibase(String changeObject, Database database) {
        ResourceAccessor fileOpener = new JUnitResourceAccessor();
        database.resetInternalState();
        return new Liquibase(FileUtils.buildPathToChangeLogFile(changeObject), fileOpener, database);
    }

    static List<String> toSqlFromLiquibaseChangeSets(Liquibase liquibase) {
        Database db = liquibase.getDatabase()
        List<ChangeSet> changeSets = liquibase.getDatabaseChangeLog().getChangeSets()
        List<String> stringList = new ArrayList<>()
        changeSets.each { stringList.addAll(toSql(it, db)) }
        return stringList
    }

    private static List<String> toSql(ChangeSet changeSet, Database db) {
        return toSql(changeSet.getChanges(), db)
    }

    private static List<String> toSql(List<? extends Change> changes, Database db) {
        List<String> stringList = new ArrayList<>()
        changes.each { stringList.addAll(toSql(it, db)) }
        return stringList
    }

    private static List<String> toSql(Change change, Database db) {
        Sql[] sqls = SqlGeneratorFactory.newInstance().generateSql(change, db)
        return sqls*.toSql()
    }

    static ArrayList<CatalogAndSchema> getCatalogAndSchema(Database database, String dbSchema) {
        List<String> schemaList = TestUtils.parseValuesToList(dbSchema, ",")

        List<CatalogAndSchema> finalList = new ArrayList<>()
        schemaList?.each { sch ->
            String[] catSchema = sch.split("\\.")
            String catalog, schema
            if (catSchema.length == 2) {
                catalog = catSchema[0]?.trim()
                schema = catSchema[1]?.trim()
            } else if (catSchema.length == 1) {
                catalog = null
                schema = catSchema[0]?.trim()
            } else {
                return finalList
            }
            finalList.add(new CatalogAndSchema(catalog, schema).customize(database))
        }

        return finalList
    }

    static List<String> parseValuesToList(String str, String regex = null) {
        List<String> returnList = new ArrayList<>()
        if (str) {
            if (regex == null) {
                returnList.add(str)
                return returnList
            }
            return str?.split(regex)*.trim()
        }
        return new ArrayList<String>()
    }

    static List<TestInput> buildTestInput(TestConfig config) {
        List<TestInput> inputList = new ArrayList<>();
        for (DatabaseUnderTest databaseUnderTest : config.getDatabasesUnderTest()) {
            for (DatabaseVersion databaseVersion : databaseUnderTest.getVersions()) {
                if(!databaseUnderTest.getChangeObjects().isEmpty()){
                    for (String changeObject : databaseUnderTest.getChangeObjects()) {
                        inputList.add(TestInput.builder()
                                .databaseName(databaseUnderTest.getName())
                                .url(databaseVersion.getUrl())
                                .dbSchema(databaseUnderTest.getDbSchema())
                                .username(databaseUnderTest.getUsername())
                                .password(databaseUnderTest.getPassword())
                                .version(databaseVersion.getVersion())
                                .context(config.getContext())
                                .changeObject(changeObject)
                                .build()
                        )
                    }
                }
                else {
                    // TODO version specific changeObjects
                }
            }
        }
        return inputList;
    }

}
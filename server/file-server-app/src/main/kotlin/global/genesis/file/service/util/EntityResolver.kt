package global.genesis.file.service.util

import global.genesis.commons.annotation.Module
import global.genesis.db.DbRecord
import global.genesis.db.dictionary.util.classForName
import global.genesis.db.entity.TableEntity
import global.genesis.db.entity.ViewEntity
import global.genesis.dictionary.pal.GPalTable
import global.genesis.dictionary.pal.IndexType
import global.genesis.dictionary.pal.view.GPalView
import global.genesis.pal.repo.RepoProvider
import global.genesis.pal.view.repo.helper.set
import kotlinx.coroutines.rx3.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject

private const val CONFIG_VIEW_PACKAGE = "global.genesis.gen.config.view."
private const val CONFIG_TABLE_PACKAGE = "global.genesis.gen.config.tables."

sealed class EntityResult {
    data class ViewResult(
        val view: GPalView<ViewEntity>
    ) : EntityResult()

    data class TableResult(
        val table: GPalTable<TableEntity>
    ) : EntityResult()

    data object NoResult : EntityResult()
}

/**
 * Utility class for resolving a db entity given only the entity name
 * in UPPER_CAMEL_CASE and a string representation of its primary key.
 *
 * @author tgosling
 */
@Module
class EntityResolver @Inject constructor(
    val repoProvider: RepoProvider
) {
    fun resolveEntity(entityName: String?): EntityResult {
        if (entityName.isNullOrBlank()) {
            return EntityResult.NoResult
        }

        val kClass1 = classForName("${CONFIG_VIEW_PACKAGE}$entityName")
        if (kClass1 != null && GPalView::class.java.isAssignableFrom(kClass1)) {
            return EntityResult.ViewResult((kClass1.kotlin.objectInstance as GPalView<ViewEntity>))
        }

        val kClass2 = classForName("${CONFIG_TABLE_PACKAGE}$entityName")
        if (kClass2 != null && GPalTable::class.java.isAssignableFrom(kClass2)) {
            return EntityResult.TableResult(kClass2.kotlin.objectInstance as GPalTable<TableEntity>)
        }

        return EntityResult.NoResult
    }

    suspend fun getViewEntity(
        entityResult: EntityResult.ViewResult,
        entityId: String
    ): Any {
        val gPalView = entityResult.view
        val unsafeRepo = repoProvider.invoke(gPalView).unsafeRepo
        val primaryIndex = gPalView.indices.first { it.indexType == IndexType.PRIMARY }
        val tableName = gPalView.rootTableName
        val record = DbRecord(tableName)
        if (primaryIndex.fieldNames.size != 1) {
            throw Exception("Could not find ViewResult $entityResult for index $primaryIndex")
        } else {
            record[primaryIndex.fieldNames.first()] = entityId
        }

        return try {
            unsafeRepo.get(primaryIndex.name, record).awaitSingle()
        } catch (e: Exception) {
            LOG.warn("Record could not be found in table: $tableName for id: $entityId")
            EntityResult.NoResult
        }
    }

    suspend fun getTableEntity(
        entityResult: EntityResult.TableResult,
        entityId: String
    ): Any {
        val gPalTable = entityResult.table
        val unsafeRepo = repoProvider.invoke(gPalTable).unsafeRepo
        val primaryIndex = gPalTable.indices.first { it.indexType == IndexType.PRIMARY }
        val tableName = gPalTable.name
        val record = DbRecord(tableName)
        if (primaryIndex.fieldNames.size != 1) {
            throw Exception("Could not find TableResult $entityResult for index $primaryIndex")
        } else {
            record[primaryIndex.fieldNames.first()] = entityId
        }

        return try {
            unsafeRepo.get(primaryIndex.name, record).awaitSingle()
        } catch (e: Exception) {
            LOG.warn("Record could not be found in table: $tableName for id: $entityId")
            EntityResult.NoResult
        }
    }

    private val LOG: Logger = LoggerFactory.getLogger(EntityResolver::class.java)
}

package global.genesis.file.service.util

import global.genesis.gen.dao.Trade
import global.genesis.gen.dao.User
import global.genesis.gen.dao.UserAttributes
import global.genesis.gen.view.entity.UserView
import global.genesis.pal.shared.inject
import global.genesis.testsupport.AbstractGenesisTestSupport
import global.genesis.testsupport.EventResponse
import global.genesis.testsupport.GenesisTestConfig
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class EntityResolverTest : AbstractGenesisTestSupport<EventResponse>(
    GenesisTestConfig {
        addPackageName("global.genesis.file")
        genesisHome = "/GenesisHome/"
        useTempClassloader = true
    }
) {
    private lateinit var entityResolver: EntityResolver

    override fun systemDefinition(): Map<String, Any> = mapOf(
        "IS_SCRIPT" to "true",
        "LOCAL_STORAGE_FOLDER" to "LOCAL_STORAGE",
        "STORAGE_STRATEGY" to "LOCAL"
    )

    @BeforeEach
    fun setUp() {
        entityResolver = bootstrap.injector.inject()
    }

    @Test
    fun testTableEntity(): Unit = runBlocking {
        val id = entityDb.insert(
            Trade {
                tradeId = "12345"
                currencyId = "GBP"
                price = 1.2
                quantity = 1000
                tradeDate = DateTime.now()
                tradeType = "Type"
            }
        ).record.tradeId

        val resolveEntity = entityResolver.resolveEntity("TRADE")
        assertIs<EntityResult.TableResult>(resolveEntity)
        val tableEntity = entityResolver.getTableEntity(resolveEntity, id)
        assertIs<Trade>(tableEntity)
    }

    @Test
    fun testViewEntity(): Unit = runBlocking {
        entityDb.insert(
            User {
                userName = "testUser"
            }
        ).record.userName

        entityDb.insert(
            UserAttributes {
                userName = "testUser"
            }
        ).record.userName

        val resolveEntity = entityResolver.resolveEntity("USER_VIEW")
        assertIs<EntityResult.ViewResult>(resolveEntity)

        val id = entityDb.get(UserView.ByName("testUser"))?.userName
        assertNotNull(id)
        val viewEntity = entityResolver.getViewEntity(resolveEntity, id)
        assertIs<UserView>(viewEntity)
    }

    @Test
    fun testNoResult(): Unit = runBlocking {
        val resolveNullEntity = entityResolver.resolveEntity(null)
        assertIs<EntityResult.NoResult>(resolveNullEntity)

        val resolveEmptyEntity = entityResolver.resolveEntity("")
        assertIs<EntityResult.NoResult>(resolveEmptyEntity)
    }
}

import java.util.concurrent.TimeUnit

process {
    cacheConfig {
        expireAfterAccess(7, TimeUnit.DAYS)
        expireAfterWrite(7, TimeUnit.DAYS)

        initialCapacity = 1000
        maximumEntries = 1000
        update = true
        multipleKeys = true
    }
}

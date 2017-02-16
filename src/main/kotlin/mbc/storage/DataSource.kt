package mbc.storage

/**
 * 数据源类，具体实现可以是Memory，LevelDb等。
 */
interface DataSource<K, V> {
  /**
   * 数据源的名字。
   */
  val name: String

  /**
   * 根据Key获得Value
   * @return 如果成功返回Value，如果失败返回Null。
   */
  fun get(key: K): V?

  /**
   * 写入Key-Value对。
   */
  fun put(key: K, value: V)

  /**
   * 删除Key-Value对。
   */
  fun delete(key: K)

  /**
   * 持久化数据。
   * @return 执行成功返回true，执行失败返回false
   */
  fun flush(): Boolean

  /**
   * 初始化数据库。
   */
  fun init()

  /**
   * @return 如果数据库可用则返回true
   */
  fun isAlive(): Boolean

  /**
   * 关闭数据库。
   */
  fun close()

  /**
   * 批量写入Key-Value。
   */
  fun updateBatch(rows: Map<ByteArray, V>)

  /**
   * @return 数据库的Keys，如果不支持该操作返回Null
   */
  fun keys(): Set<K>

  /**
   * @return 数据库的Keys，如果不支持该操作返回Null
   */
  fun reset()

}

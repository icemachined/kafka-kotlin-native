package com.icemachined.kafka.common.header

interface Headers : Iterable<Header> {
    /**
     * Adds a header (key inside), to the end, returning if the operation succeeded.
     *
     * @param header the Header to be added
     * @return this instance of the Headers, once the header is added.
     * @throws IllegalStateException is thrown if headers are in a read-only state.
     */
    @Throws(IllegalStateException::class)
    fun add(header: Header?): Headers?

    /**
     * Creates and adds a header, to the end, returning if the operation succeeded.
     *
     * @param key of the header to be added.
     * @param value of the header to be added.
     * @return this instance of the Headers, once the header is added.
     * @throws IllegalStateException is thrown if headers are in a read-only state.
     */
    @Throws(IllegalStateException::class)
    fun add(key: String?, value: ByteArray?): Headers?

    /**
     * Removes all headers for the given key returning if the operation succeeded.
     *
     * @param key to remove all headers for.
     * @return this instance of the Headers, once the header is removed.
     * @throws IllegalStateException is thrown if headers are in a read-only state.
     */
    @Throws(IllegalStateException::class)
    fun remove(key: String?): Headers?

    /**
     * Returns just one (the very last) header for the given key, if present.
     *
     * @param key to get the last header for.
     * @return this last header matching the given key, returns none if not present.
     */
    fun lastHeader(key: String?): Header?

    /**
     * Returns all headers for the given key, in the order they were added in, if present.
     *
     * @param key to return the headers for.
     * @return all headers for the given key, in the order they were added in, if NO headers are present an empty iterable is returned.
     */
    fun headers(key: String?): Iterable<Header?>?

    /**
     * Returns all headers as an array, in the order they were added in.
     *
     * @return the headers as a Header[], mutating this array will not affect the Headers, if NO headers are present an empty array is returned.
     */
    fun toArray(): Array<Header?>?
}
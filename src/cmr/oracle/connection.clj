(ns cmr.oracle.connection
  "Contains a record definition that implements the ConcpetStore and Lifecycle protocols
  backed by an Oracle database."
  (:require [cmr.common.lifecycle :as lifecycle]
            [cmr.common.log :refer (debug info warn error)]
            [clojure.java.jdbc :as j])
  (:import oracle.ucp.jdbc.PoolDataSourceFactory
           oracle.ucp.admin.UniversalConnectionPoolManagerImpl))


(defn db-spec
  [connection-pool-name db-url fcf-enabled ons-config user password]
  {:classname "oracle.jdbc.pool.OracleDataSource"
   :subprotocol "oracle"
   :subname db-url
   :user user
   :password password
   :connection-pool-name connection-pool-name
   :fcf-enabled fcf-enabled
   :ons-config ons-config})


(defn test-db-connection!
  "Tests the database connection. Throws an exception if unable to execute some sql."
  [oracle-store]
  (try
    (when-not (= [{:a 1M}]
                 (j/query oracle-store "select 1 a from dual"))
      (throw (Exception. "Could not select data from database.")))
    (catch Exception e
      (info "Database conn info" (pr-str (-> oracle-store
                                             :spec
                                             (assoc :password "*****"))))
      (throw e))))

(defn pool
  [spec]
  (let [{:keys [classname
                subprotocol
                subname
                user
                password
                connection-pool-name
                fcf-enabled
                ons-config]} spec]
    (doto (PoolDataSourceFactory/getPoolDataSource)
      (.setConnectionFactoryClassName classname)
      (.setConnectionPoolName connection-pool-name)
      (.setUser user)
      (.setPassword password)
      (.setURL (str "jdbc:" subprotocol ":" subname))
      (.setMinPoolSize 5)
      (.setMaxPoolSize 100)
      (.setInactiveConnectionTimeout 60)
      (.setConnectionWaitTimeout 600)
      (.setPropertyCycle 120)
      (.setFastConnectionFailoverEnabled fcf-enabled)
      (.setONSConfiguration ons-config)
      (.setAbandonedConnectionTimeout 3500)
      (.setValidateConnectionOnBorrow true)
      (.setSQLForValidateConnection "select 1 from DUAL"))))

(defrecord OracleStore
  [
   ;; The database spec.
   spec

   ;; The database pool of connections
   datasource

   ]

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  lifecycle/Lifecycle

  (start [this system]
         (let [this (assoc this :datasource (pool spec))]
           (test-db-connection! this)
           this))

  (stop [this system]

        (try
          ;; Cleanup the connection pool
          (let [pool-name (get-in this [:spec :connection-pool-name])
                ucp-manager (UniversalConnectionPoolManagerImpl/getUniversalConnectionPoolManager)]
            (.destroyConnectionPool ucp-manager pool-name))
          (catch Exception e
            (warn (str "Unable to destroy connection pool. It may not have started. Error: "
                       (.getMessage e)))))

        (dissoc this :datasource)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-db
  "Creates and returns the database connection pool."
  [spec]
  (->OracleStore spec nil))
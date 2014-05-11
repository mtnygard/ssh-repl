(ns ssh-repl.server
  (:require [clojure.java.io :as io]
            [ssh-repl.pubkey :as pubkey])
  (:import [org.apache.sshd SshServer]
           [org.apache.sshd.server Command PasswordAuthenticator PublickeyAuthenticator]
           [org.apache.sshd.common Factory]
           [org.apache.sshd.server.keyprovider SimpleGeneratorHostKeyProvider]))

(defn- default-server
  [hostkey-path host port]
  (doto (SshServer/setUpDefaultServer)
    (.setPort port)
    (.setHost host)
    (.setKeyPairProvider (SimpleGeneratorHostKeyProvider. hostkey-path))))

(def ^:private shell-factory
  (reify Factory
    (create [this]
      (let [state (atom {})]
        (reify Command
          (destroy [this]
            (when-let [fut (:future @state)]
              (future-cancel fut)))
          (setErrorStream [this err]
            (.setNoDelay err true)
            (swap! state assoc-in [:streams :err] err))
          (setExitCallback [this cb]
            (swap! state assoc :exit-callback cb))
          (setInputStream [this in]
            (swap! state assoc-in [:streams :in] in))
          (setOutputStream [this out]
            (.setNoDelay out true)
            (swap! state assoc-in [:streams :out] out))
          (start [this env]
            (binding [*in* (-> @state
                               :streams
                               :in
                               io/reader
                               clojure.lang.LineNumberingPushbackReader.)
                      *out* (-> @state
                                :streams
                                :out
                                io/writer)
                      *err* (-> @state
                                :streams
                                :err
                                io/writer)]
              (swap! state
                     assoc
                     :future
                     (future-call (bound-fn* clojure.main/repl))))))))))

(defmulti ^:private set-authenticator (fn [_ type _] type))

(defmethod set-authenticator :password
  [daemon _ resolver]
  (doto daemon
    (.setPasswordAuthenticator
     (reify PasswordAuthenticator
       (authenticate [this username password session]
         (= password (resolver username)))))))

(defmethod set-authenticator :public-key
  [daemon _ resolver]
  (doto daemon
    (.setPublickeyAuthenticator
     (reify PublickeyAuthenticator
       (authenticate [this username key session]
         (let [allowed-keys (pubkey/read-ssh-keys (resolver username))]
           (not (nil? (some (fn [allowed-key]
                              (= key allowed-key))
                            allowed-keys)))))))))

(defn start-repl
  ([authentication-type port username-to-credentials-fn]
     (start-repl authentication-type nil port username-to-credentials-fn))
  ([authentication-type host port username-to-credentials-fn]
     (let [server (default-server "hostkey.ser" host port)]
       (.setShellFactory server shell-factory)
       (set-authenticator server authentication-type username-to-credentials-fn)
       (.start server)
       server)))

(defn stop-repl
  [server]
  (.stop server true))


(comment

  (def s (start-repl :password 2022 (constantly "abcdef")))
  (stop-repl s)

  (def s (start-repl :public-key 2022 (constantly "file:///Users/mtnygard/.ssh/authorized_keys")))
  (stop-repl s)

  (def a  (authenticator :public-key (constantly "file://Users/mtnygard/.ssh/authorized_keys")))


)

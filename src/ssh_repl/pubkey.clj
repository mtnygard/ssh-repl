(ns ssh-repl.pubkey
  (:import [java.math BigInteger]
           [java.security KeyFactory PublicKey]
           [java.security.spec DSAPublicKeySpec RSAPublicKeySpec]
           [java.util Scanner]))

(defn decode-string
  "Decodes a string from a ByteBuffer."
  [bb]
  (let [len (.getInt bb)
        buf (byte-array len)]
    (.get bb buf)
    (String. buf)))

(defn decode-bigint
  "Decodes a java.math.BigInteger from a ByteBuffer."
  [bb]
  (let [len (.getInt bb)
        buf (byte-array len)]
    (.get bb buf)
    (BigInteger. buf)))

(defn read-ssh-key
  "Reads in the SSH key at `path`, returning an instance of
  `java.security.PublicKey`."
  [path]
  (let [contents (slurp path)
        parts    (clojure.string/split contents #" ")
        bytes    (->> parts
                      (filter #(.startsWith % "AAAA"))
                      first
                      javax.xml.bind.DatatypeConverter/parseBase64Binary)
        bb       (-> bytes
                     alength
                     java.nio.ByteBuffer/allocate
                     (.put bytes)
                     .flip)]
    (case (decode-string bb)
      "ssh-rsa" (.generatePublic (KeyFactory/getInstance "RSA")
                                 (let [[e m] (repeatedly 2 #(decode-bigint bb))]
                                      (RSAPublicKeySpec. m e)))
      "ssh-dss" (.generatePublic (KeyFactory/getInstance "DSA")
                                 (let [[p q g y] (repeatedly 4 #(decode-bigint bb))]
                                   (DSAPublicKeySpec. y p q g)))
      (throw (ex-info "Unknown key type"
                      {:reason ::unknown-key-type
                       :type   type})))))

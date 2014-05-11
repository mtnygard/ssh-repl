# SSH-REPL

SSH to a REPL

## Why?

This is a one-step solution to running a REPL with authentication.

You can absolutely use SSH to create a port tunnel. In fact, that's
certainly a better, more secure solution than this.

## Limitations

READ THIS!

* This library doesn't know how to use PAM, LDAP, or any existing
  host-based authentication.
* This library does not provide any authorization. Once a REPL is
  started, it can run any Clojure code you submit.
* The very purpose of this library is remote code execution of
  arbitrary code.

Should you choose to use this library, do so with the full knowledge
that you will not pass a PCI audit, SAS 70, or even a cursory security
review.

## Installation

SSH-REPL is available from clojars. Add this `:dependency` to your
`project.clj`:

    [mtnygard/ssh-repl "0.1.0-SNAPSHOT"]

## Starting the server

You have a choice to make. Do you want to use password authentication
or do you want to use a public key?

With public key:

    (start-server :public-key port username-to-key-fn)
    ;; or
    (start-server :public-key host port username-to-key-fn)

Without the host argument, the SSHD will listen on all interfaces on
the host. With it, the daemon will only listen on the interface that
corresponds to that hostname or IP address.

The `username-to-key-fn` is a function you must provide to map a
username into a URL where authorized public keys for that user may be
found.

If you want to use home directories of known users, you can supply
`ssh-repl.resolver/sshd-authorized-keys`, which uses
`$HOME/.ssh/authorized-keys` as the place to find keys.

With password:

    (start-server :password port username-to-hash-fn)
    ;; or
    (start-server :password host port username-to-hash-fn)

`username-to-hash-fn` is a function that looks up the hashed password
value for a user. If this is returns a static string, then your hashed
password resides in memory and source code forever. Not a good idea,
but you could do it. Be sure to
[read up on hashing](https://crackstation.net/hashing-security.htm).

Keeping a proper user directory is way outside the scope of this
library.

## Stopping the server

Both forms of `start-server` return the same thing, a value that you
can pass to `stop-server`.

    (stop-server s)

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
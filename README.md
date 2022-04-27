# Signal-Backup-Decrypt

This is a very simple Signal decryptor that works as of May 2022. It decrypts attachments (including voice notes) and exports messages, reactions, and quotes to a simple JSON file. This codebase relies on the Signal source code to decrypt backups instead of attempting to reinvent the wheel in Python or Go. If the Signal backup format changes, then you'll have to modify the files in `protos/` and `src/main/java/securesms/backup/{FullBackupBase,FullBackupImporter}.java` to account for that.

To run this, download the .zip file from distribution, extract it, and run the command with the appropriate filename and passphrase:
```sh
bin/Signal-Backup-Decrypt signal-*.backup '12345 22345 32345 42345 52345 62345'
```

Your output will be dumped to `out/`, with each message thread in its own folder. Run this on an SSD if you can, and if you run it in WSL, make sure that its run on the Linux file tree, not Windows. Otherwise it will be painfully slow.


## Development

This was developed with Java 17 and the `gradle` build system. You can build this with:
```
./gradlew run --args="-v -r signal-2024-02-29-00-00-00.backup '12345 22345 32345 42345 52345 62345'"
```
Where `-v` makes it verbose (does nothing currently) and `-r` also dumps a lot of raw output for your perusal, including database schema necessary for debugging.

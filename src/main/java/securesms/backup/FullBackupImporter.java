package securesms.backup;

import exports.Exporter;

import core.util.Conversions;
import core.util.StreamUtil;
import libsignal.protocol.kdf.HKDFv3;
import libsignal.protocol.util.ByteUtil;
import securesms.backup.BackupProtos.Attachment;
import securesms.backup.BackupProtos.BackupFrame;
import securesms.backup.BackupProtos.SharedPreference;
import securesms.backup.BackupProtos.SqlStatement;
import securesms.backup.BackupProtos.Sticker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FullBackupImporter extends FullBackupBase {

  public static void importFile(String file, String passphrase, Path outdir) throws IOException {
    InputStream is = new FileInputStream(new File(Objects.requireNonNull(file)));
    importFile(is, passphrase, outdir);
  }

  public static void importFile(InputStream is, String passphrase, Path outdir) throws IOException {
    int count = 0;
  
    Exporter exporter = new Exporter(outdir);
    BackupRecordInputStream inputStream = new BackupRecordInputStream(is, passphrase);

    BackupFrame frame;

    while (!(frame = inputStream.readFrame()).getEnd()) {
      count++;

      if (count % 1000 == 0)
        System.err.println(String.format("FRAME\t %d", count));

      if (frame.hasVersion())
        continue;
      else if (frame.hasStatement())
        processStatement(exporter, frame.getStatement());
      else if (frame.hasPreference())
        processPreference(exporter, frame.getPreference());
      else if (frame.hasAttachment())
        processAttachment(exporter, frame.getAttachment(), inputStream);
      else if (frame.hasSticker())
        processSticker(exporter, frame.getSticker(), inputStream);
      else if (frame.hasAvatar())
        processAvatar(exporter, frame.getAvatar(), inputStream);
      else if (frame.hasKeyValue())
        processKeyValue(exporter, frame.getKeyValue());
      else
        count--;
    }

  }

  private static void processStatement(Exporter exporter, SqlStatement statement) {
    exporter.writeAppendFile("statements.sql", statement.toString().getBytes());
  }

  private static void processAttachment(Exporter exporter, Attachment attachment, BackupRecordInputStream inputStream)
      throws IOException {
        System.err.println(String.format("Attachment > %s", attachment.getAttachmentId()));

        /*
    File dataFile = AttachmentDatabase.newFile(context);
    Pair<byte[], OutputStream> output = ModernEncryptingPartOutputStream.createFor(attachmentSecret, dataFile, false);

    ContentValues contentValues = new ContentValues();

    try {
      inputStream.readAttachmentTo(output.second, attachment.getLength());

      contentValues.put(AttachmentDatabase.DATA, dataFile.getAbsolutePath());
      contentValues.put(AttachmentDatabase.DATA_RANDOM, output.first);
    } catch (BadMacException e) {
      Log.w(TAG, "Bad MAC for attachment " + attachment.getAttachmentId() + "! Can't restore it.", e);
      dataFile.delete();
      contentValues.put(AttachmentDatabase.DATA, (String) null);
      contentValues.put(AttachmentDatabase.DATA_RANDOM, (String) null);
    }

    db.update(AttachmentDatabase.TABLE_NAME, contentValues,
        AttachmentDatabase.ROW_ID + " = ? AND " + AttachmentDatabase.UNIQUE_ID + " = ?",
        new String[] { String.valueOf(attachment.getRowId()), String.valueOf(attachment.getAttachmentId()) });
        */
  }

  private static void processSticker(Exporter exporter, Sticker sticker, BackupRecordInputStream inputStream)
      throws IOException {
        /*
    File stickerDirectory = context.getDir(StickerDatabase.DIRECTORY, Context.MODE_PRIVATE);
    File dataFile = File.createTempFile("sticker", ".mms", stickerDirectory);

    Pair<byte[], OutputStream> output = ModernEncryptingPartOutputStream.createFor(attachmentSecret, dataFile, false);

    inputStream.readAttachmentTo(output.second, sticker.getLength());

    ContentValues contentValues = new ContentValues();
    contentValues.put(StickerDatabase.FILE_PATH, dataFile.getAbsolutePath());
    contentValues.put(StickerDatabase.FILE_LENGTH, sticker.getLength());
    contentValues.put(StickerDatabase.FILE_RANDOM, output.first);

    db.update(StickerDatabase.TABLE_NAME, contentValues,
        StickerDatabase._ID + " = ?",
        new String[] { String.valueOf(sticker.getRowId()) });
        */
  }

  private static void processAvatar(Exporter exporter, BackupProtos.Avatar avatar, BackupRecordInputStream inputStream) throws IOException {
    /*
    if (avatar.hasRecipientId()) {
      RecipientId recipientId = RecipientId.from(avatar.getRecipientId());
      inputStream.readAttachmentTo(AvatarHelper.getOutputStream(context, recipientId, false), avatar.getLength());
    } else {
      if (avatar.hasName() && SqlUtil.tableExists(db, "recipient_preferences")) {
        Log.w(TAG,
            "Avatar is missing a recipientId. Clearing signal_profile_avatar (legacy) so it can be fetched later.");
        db.execSQL("UPDATE recipient_preferences SET signal_profile_avatar = NULL WHERE recipient_ids = ?",
            new String[] { avatar.getName() });
      } else if (avatar.hasName() && SqlUtil.tableExists(db, "recipient")) {
        Log.w(TAG, "Avatar is missing a recipientId. Clearing signal_profile_avatar so it can be fetched later.");
        db.execSQL("UPDATE recipient SET signal_profile_avatar = NULL WHERE phone = ?",
            new String[] { avatar.getName() });
      } else {
        Log.w(TAG, "Avatar is missing a recipientId. Skipping avatar restore.");
      }

      inputStream.readAttachmentTo(new ByteArrayOutputStream(), avatar.getLength());
    }
    */
  }

  private static void processKeyValue(Exporter exporter, BackupProtos.KeyValue keyValue) {
    /*
    KeyValueDataSet dataSet = new KeyValueDataSet();

    if (keyValue.hasBlobValue()) {
      dataSet.putBlob(keyValue.getKey(), keyValue.getBlobValue().toByteArray());
    } else if (keyValue.hasBooleanValue()) {
      dataSet.putBoolean(keyValue.getKey(), keyValue.getBooleanValue());
    } else if (keyValue.hasFloatValue()) {
      dataSet.putFloat(keyValue.getKey(), keyValue.getFloatValue());
    } else if (keyValue.hasIntegerValue()) {
      dataSet.putInteger(keyValue.getKey(), keyValue.getIntegerValue());
    } else if (keyValue.hasLongValue()) {
      dataSet.putLong(keyValue.getKey(), keyValue.getLongValue());
    } else if (keyValue.hasStringValue()) {
      dataSet.putString(keyValue.getKey(), keyValue.getStringValue());
    } else {
      Log.i(TAG, "Unknown KeyValue backup value, skipping");
      return;
    }

    KeyValueDatabase.getInstance(ApplicationDependencies.getApplication()).writeDataSet(dataSet,
        Collections.emptyList());
        */
  }

  private static void processPreference(Exporter exporter, SharedPreference preference) {
    /*
    SharedPreferences preferences = context.getSharedPreferences(preference.getFile(), 0);

    // Identity keys were moved from shared prefs into SignalStore. Need to handle
    // importing backups made before the migration.
    if ("SecureSMS-Preferences".equals(preference.getFile())) {
      if ("pref_identity_public_v3".equals(preference.getKey()) && preference.hasValue()) {
        SignalStore.account().restoreLegacyIdentityPublicKeyFromBackup(preference.getValue());
      } else if ("pref_identity_private_v3".equals(preference.getKey()) && preference.hasValue()) {
        SignalStore.account().restoreLegacyIdentityPrivateKeyFromBackup(preference.getValue());
      }

      return;
    }

    if (preference.hasValue()) {
      preferences.edit().putString(preference.getKey(), preference.getValue()).commit();
    } else if (preference.hasBooleanValue()) {
      preferences.edit().putBoolean(preference.getKey(), preference.getBooleanValue()).commit();
    } else if (preference.hasIsStringSetValue() && preference.getIsStringSetValue()) {
      preferences.edit().putStringSet(preference.getKey(), new HashSet<>(preference.getStringSetValueList())).commit();
    }
    */
  }

  private static class BackupRecordInputStream extends BackupStream {

    private final InputStream in;
    private final Cipher cipher;
    private final Mac mac;

    private final byte[] cipherKey;
    private final byte[] macKey;

    private byte[] iv;
    private int counter;

    private BackupRecordInputStream(InputStream in, String passphrase) throws IOException {
      try {
        this.in = in;

        byte[] headerLengthBytes = new byte[4];
        StreamUtil.readFully(in, headerLengthBytes);

        int headerLength = Conversions.byteArrayToInt(headerLengthBytes);
        byte[] headerFrame = new byte[headerLength];
        StreamUtil.readFully(in, headerFrame);

        BackupFrame frame = BackupFrame.parseFrom(headerFrame);

        if (!frame.hasHeader()) {
          throw new IOException("Backup stream does not start with header!");
        }

        BackupProtos.Header header = frame.getHeader();

        this.iv = header.getIv().toByteArray();

        if (iv.length != 16) {
          throw new IOException("Invalid IV length!");
        }

        byte[] key = getBackupKey(passphrase, header.hasSalt() ? header.getSalt().toByteArray() : null);
        byte[] derived = new HKDFv3().deriveSecrets(key, "Backup Export".getBytes(), 64);
        byte[][] split = ByteUtil.split(derived, 32, 32);

        this.cipherKey = split[0];
        this.macKey = split[1];

        this.cipher = Cipher.getInstance("AES/CTR/NoPadding");
        this.mac = Mac.getInstance("HmacSHA256");
        this.mac.init(new SecretKeySpec(macKey, "HmacSHA256"));

        this.counter = Conversions.byteArrayToInt(iv);
      } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
        throw new AssertionError(e);
      }
    }

    BackupFrame readFrame() throws IOException {
      return readFrame(in);
    }

    void readAttachmentTo(OutputStream out, int length) throws IOException {
      try {
        Conversions.intToByteArray(iv, 0, counter++);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cipherKey, "AES"), new IvParameterSpec(iv));
        mac.update(iv);

        byte[] buffer = new byte[8192];

        while (length > 0) {
          int read = in.read(buffer, 0, Math.min(buffer.length, length));
          if (read == -1)
            throw new IOException("File ended early!");

          mac.update(buffer, 0, read);

          byte[] plaintext = cipher.update(buffer, 0, read);

          if (plaintext != null) {
            out.write(plaintext, 0, plaintext.length);
          }

          length -= read;
        }

        byte[] plaintext = cipher.doFinal();

        if (plaintext != null) {
          out.write(plaintext, 0, plaintext.length);
        }

        out.close();

        byte[] ourMac = ByteUtil.trim(mac.doFinal(), 10);
        byte[] theirMac = new byte[10];

        try {
          StreamUtil.readFully(in, theirMac);
        } catch (IOException e) {
          throw new IOException(e);
        }

        if (!MessageDigest.isEqual(ourMac, theirMac)) {
          throw new BadMacException();
        }
      } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
          | BadPaddingException e) {
        throw new AssertionError(e);
      }
    }

    private BackupFrame readFrame(InputStream in) throws IOException {
      try {
        byte[] length = new byte[4];
        StreamUtil.readFully(in, length);

        byte[] frame = new byte[Conversions.byteArrayToInt(length)];
        StreamUtil.readFully(in, frame);

        byte[] theirMac = new byte[10];
        System.arraycopy(frame, frame.length - 10, theirMac, 0, theirMac.length);

        mac.update(frame, 0, frame.length - 10);
        byte[] ourMac = ByteUtil.trim(mac.doFinal(), 10);

        if (!MessageDigest.isEqual(ourMac, theirMac)) {
          throw new IOException("Bad MAC");
        }

        Conversions.intToByteArray(iv, 0, counter++);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cipherKey, "AES"), new IvParameterSpec(iv));

        byte[] plaintext = cipher.doFinal(frame, 0, frame.length - 10);

        return BackupFrame.parseFrom(plaintext);
      } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
          | BadPaddingException e) {
        throw new AssertionError(e);
      }
    }
  }

  private static class BadMacException extends IOException {
  }

  public static class DatabaseDowngradeException extends IOException {
    DatabaseDowngradeException(int currentVersion, int backupVersion) {
      super(
          "Tried to import a backup with version " + backupVersion + " into a database with version " + currentVersion);
    }
  }
}

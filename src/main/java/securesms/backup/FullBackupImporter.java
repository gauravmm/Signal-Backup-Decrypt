package securesms.backup;

import exports.AttachmentMetadata;
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
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.StatementEvent;

import org.codehaus.jackson.map.util.JSONPObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class FullBackupImporter extends FullBackupBase {
	private boolean dump_raw;

	public static void importFile(String file, String passphrase, Path outdir, Boolean dump_raw) throws IOException {
		InputStream is = new FileInputStream(new File(Objects.requireNonNull(file)));
		importFile(is, passphrase, outdir, dump_raw);
	}

	public static void importFile(InputStream is, String passphrase, Path outdir, Boolean dump_raw) throws IOException {
		int count = 0;

		Exporter exporter = new Exporter(outdir, true);
		BackupRecordInputStream inputStream = new BackupRecordInputStream(is, passphrase);

		BackupFrame frame;

		while (!(frame = inputStream.readFrame()).getEnd()) {
			count++;

			if (count % 1000 == 0)
				System.err.println(String.format("FRAME\t %d", count));

			if (frame.hasVersion())
				continue;
			else if (frame.hasAttachment())
				processAttachment(exporter, frame.getAttachment(), inputStream);
			else if (frame.hasStatement())
				processStatement(exporter, frame.getStatement(), dump_raw);
			else if (dump_raw && frame.hasPreference())
				processPreference(exporter, frame.getPreference());
			else if (dump_raw && frame.hasSticker())
				processSticker(exporter, frame.getSticker(), inputStream);
			else if (dump_raw && frame.hasAvatar())
				processAvatar(exporter, frame.getAvatar(), inputStream);
			else if (dump_raw && frame.hasKeyValue())
				processKeyValue(exporter, frame.getKeyValue());
			else
				count--;
		}

	}

	private static void processStatement(Exporter exporter, SqlStatement statement, Boolean dump_raw) {
		if (statement.getStatement().startsWith("CREATE")) {
			if (dump_raw)
				exporter.writeAppendFile("raw/_schema.sql", (statement.getStatement() + "\n").getBytes());
			return;
		}

		// Prepare the parameters.
		String[] parameters = new String[statement.getParametersCount()];
		int i = 0;
		for (SqlStatement.SqlParameter parameter : statement.getParametersList()) {
			if (parameter.hasStringParamter())
				parameters[i] = (parameter.getStringParamter());
			else if (parameter.hasDoubleParameter())
				parameters[i] = (Double.toString(parameter.getDoubleParameter()));
			else if (parameter.hasIntegerParameter())
				parameters[i] = (Long.toString(parameter.getIntegerParameter()));
			else if (parameter.hasBlobParameter())
				parameters[i] = (parameter.getBlobParameter().toByteArray().toString());
			else if (parameter.hasNullparameter())
				parameters[i] = ("");
			i++;
		}

		if (statement.getStatement().startsWith("INSERT INTO part")) {
			String[] mapKeys = { "_id", "mid", null, "ct", "name", null, null, null, null, null, null, null,
					null, null, null, null, "file_name", null, null, "unique_id", null, null, "voice_note", null, null,
					null, "height", null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null };

			Map<String, String> m = assembleMap(mapKeys, parameters);

			if (dump_raw) {
				OutputStream out = exporter
						.getFileStream("raw/part.json");
				try {
					out.write((JSONObject.toJSONString(m) + "\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} else if (statement.getStatement().startsWith("INSERT INTO reaction")) {
			String[] mapKeys = { "_id", "message_id", "is_mms", "author_id", "emoji", "date_sent", "date_received" };

			Map<String, String> m = assembleMap(mapKeys, parameters);
			if (dump_raw) {
				OutputStream out = exporter
						.getFileStream("raw/messages/reaction.json");
				try {
					out.write((JSONObject.toJSONString(m) + "\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} else if (statement.getStatement().startsWith("INSERT INTO sms")) {
			String[] mapKeys = { "_id", "thread_id", null, null, "person", "date", null, null, null, null, null, "type",
					null, null, null, "body", null, null, null, null, null, null, null, null, null, null, null,
					"remote_deleted", null, null, null
			};

			Map<String, String> m = assembleMap(mapKeys, parameters);
			if (dump_raw) {
				OutputStream out = exporter
						.getFileStream(String.format("raw/messages/thread_%3s.json", m.get("thread_id")));
				try {
					out.write((JSONObject.toJSONString(m) + "\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} else if (statement.getStatement().startsWith("INSERT INTO mms")) {
			String[] mapKeys = { "_id", "thread_id", "date", null, null, null, "read", "body",
					null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, "quote_id", "quote_author", "quote_body", null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
			};

			Map<String, String> m = assembleMap(mapKeys, parameters);
			if (dump_raw) {
				OutputStream out = exporter
						.getFileStream(String.format("raw/messages/thread_%3s.json", m.get("thread_id")));
				try {
					out.write((JSONObject.toJSONString(m) + "\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		// The remaining tables are only to be processed if dump_raw is set
		if (!dump_raw)
			return;

		if (statement.getStatement().startsWith("INSERT INTO drafts")) {

		} else if (statement.getStatement().startsWith("INSERT INTO push")) {

		} else if (statement.getStatement().startsWith("INSERT INTO sticker")) {

		} else if (statement.getStatement().startsWith("INSERT INTO recipient")) {

		} else if (statement.getStatement().startsWith("INSERT INTO thread")) {

		} else if (statement.getStatement().startsWith("INSERT INTO identities")) {

		} else if (statement.getStatement().startsWith("INSERT INTO distribution_list")) {

		} else if (statement.getStatement().startsWith("INSERT INTO msl_")) {
			if (statement.getStatement().startsWith("INSERT INTO msl_payload")) {
				String[] raw_keys = { "_id", "date_sent", "content", "content_hint" };
				dumpJSONToFile(exporter.getFileStream("raw/msl_payload.json"), raw_keys, parameters);

			} else if (statement.getStatement().startsWith("INSERT INTO msl_recipient")) {
				String[] raw_keys = { "_id", "payload_id", "recipient_id", "device" };
				dumpJSONToFile(exporter.getFileStream("raw/msl_recipient.json"), raw_keys, parameters);

			} else if (statement.getStatement().startsWith("INSERT INTO msl_message")) {
				String[] raw_keys = { "_id", "payload_id", "message_id", "is_mms" };
				dumpJSONToFile(exporter.getFileStream("raw/msl_message.json"), raw_keys, parameters);

			} else {
				System.err.print("Statement skipped > ");
				System.err.println(statement.getStatement());
				exporter.writeAppendFile("statements_msl.bin", statement.toByteArray());
			}

		} else {
			System.err.print("Statement skipped > ");
			System.err.println(statement.getStatement());
			exporter.writeAppendFile("statements.bin", statement.toByteArray());

		}
	}

	private static Map<String, String> assembleMap(String[] keys, String[] values) {
		Map<String, String> rv = new HashMap<>();
		for (int i = 0; i < keys.length; ++i)
			if (keys[i] != null)
				rv.put(keys[i], values[i]);

		// Special extra things:
		String[] autoremoves_zero = { "remote_deleted", "quote_id" };
		for (String autoremove : autoremoves_zero)
			if ("0".equals(rv.get(autoremove)))
				rv.remove(autoremove);

		String[] autoremoves_empty = { "shared_contacts", "quote_author", "quote_body", "name", "file_name",
				"voice_note" };
		for (String autoremove : autoremoves_empty)
			if ("".equals(rv.get(autoremove)))
				rv.remove(autoremove);

		return rv;
	}

	private static void dumpJSONToFile(OutputStream out, String[] keys, String[] values) {
		if (keys.length != values.length)
			System.err.println("Incorrect number of parameters: " + values.toString());

		try {
			out.write((JSONObject.toJSONString(assembleMap(keys, values)) + "\n").getBytes());
		} catch (IOException e) {
			System.err.println("Error dumping to text: " + values.toString());
		}
	}

	private static void processAttachment(Exporter exporter, Attachment attachment, BackupRecordInputStream inputStream)
			throws IOException {
		if (!true)
			return;
		try {
			inputStream.readAttachmentTo(
					exporter.writeOnceStream("raw/attachments", Long.toString(attachment.getAttachmentId())),
					attachment.getLength());
		} catch (BadMacException e) {
			System.err.println("Bad MAC for attachment " + attachment.getAttachmentId() + "! Can't restore it.");
			return;
		}
	}

	private static void processSticker(Exporter exporter, Sticker sticker, BackupRecordInputStream inputStream)
			throws IOException {

		inputStream.readAttachmentTo(
				exporter.writeOnceStream("sticker", String.format("sticker_%d.webp", sticker.getRowId())),
				sticker.getLength());
		// System.err.println(String.format("Sticker > %d", sticker.getRowId()));
	}

	private static void processAvatar(Exporter exporter, BackupProtos.Avatar avatar,
			BackupRecordInputStream inputStream)
			throws IOException {
		return;
	}

	private static void processKeyValue(Exporter exporter, BackupProtos.KeyValue keyValue) {
		exporter.writeAppendFile("raw/keyValue.txt", keyValue.toString().getBytes());
	}

	private static void processPreference(Exporter exporter, SharedPreference preference) {
		exporter.writeAppendFile("raw/preferences.txt", preference.toString().getBytes());
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
					"Tried to import a backup with version " + backupVersion + " into a database with version "
							+ currentVersion);
		}
	}
}

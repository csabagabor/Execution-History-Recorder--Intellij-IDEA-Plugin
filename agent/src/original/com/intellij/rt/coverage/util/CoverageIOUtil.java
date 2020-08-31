package original.com.intellij.rt.coverage.util;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoverageIOUtil {
   public static final int GIGA = 1000000000;
   private static final int STRING_HEADER_SIZE = 1;
   private static final int STRING_LENGTH_THRESHOLD = 255;
   private static final String LONGER_THAN_64K_MARKER = "LONGER_THAN_64K";
   private static final ThreadLocalCachedValue<byte[]> ioBuffer = new ThreadLocalCachedValue<byte[]>() {
      protected byte[] create() {
         return CoverageIOUtil.allocReadWriteUTFBuffer();
      }
   };
   private static final Pattern TYPE_PATTERN = Pattern.compile("(L.*;)*");

   private CoverageIOUtil() {
   }

   private static String readString(DataInput stream) throws IOException {
      int length = stream.readInt();
      if (length == -1) {
         return null;
      } else {
         char[] chars = new char[length];
         byte[] bytes = new byte[length * 2];
         stream.readFully(bytes);
         int i = 0;

         for(int i2 = 0; i < length; i2 += 2) {
            chars[i] = (char)((bytes[i2] << 8) + (bytes[i2 + 1] & 255));
            ++i;
         }

         return new String(chars);
      }
   }

   private static void writeString(DataOutput stream, String s) throws IOException {
      if (s == null) {
         stream.writeInt(-1);
      } else {
         char[] chars = s.toCharArray();
         byte[] bytes = new byte[chars.length * 2];
         stream.writeInt(chars.length);
         int i = 0;

         for(int i2 = 0; i < chars.length; i2 += 2) {
            char aChar = chars[i];
            bytes[i2] = (byte)(aChar >>> 8 & 255);
            bytes[i2 + 1] = (byte)(aChar & 255);
            ++i;
         }

         stream.write(bytes);
      }
   }

   private static byte[] allocReadWriteUTFBuffer() {
      return new byte[256];
   }

   public static void writeUTF(DataOutput storage, String value) throws IOException {
      int len = value.length();
      if (len < 255 && isAscii(value)) {
         ((byte[])ioBuffer.getValue())[0] = (byte)len;

         for(int i = 0; i < len; ++i) {
            ((byte[])ioBuffer.getValue())[i + 1] = (byte)value.charAt(i);
         }

         storage.write((byte[])ioBuffer.getValue(), 0, len + 1);
      } else {
         storage.writeByte(-1);

         try {
            storage.writeUTF(value);
         } catch (UTFDataFormatException var4) {
            storage.writeUTF("LONGER_THAN_64K");
            writeString(storage, value);
         }
      }

   }

   public static String readUTFFast(DataInput storage) throws IOException {
      int len = 255 & storage.readByte();
      if (len == 255) {
         String result = storage.readUTF();
         return "LONGER_THAN_64K".equals(result) ? readString(storage) : result;
      } else {
         char[] chars = new char[len];
         storage.readFully((byte[])ioBuffer.getValue(), 0, len);

         for(int i = 0; i < len; ++i) {
            chars[i] = (char)((byte[])ioBuffer.getValue())[i];
         }

         return new String(chars);
      }
   }

   private static boolean isAscii(String str) {
      for(int i = 0; i != str.length(); ++i) {
         char c = str.charAt(i);
         if (c >= 128) {
            return false;
         }
      }

      return true;
   }

   public static int readINT(DataInput record) throws IOException {
      int val = record.readUnsignedByte();
      if (val < 192) {
         return val;
      } else {
         int res = val - 192;
         int sh = 6;

         while(true) {
            int next = record.readUnsignedByte();
            res |= (next & 127) << sh;
            if ((next & 128) == 0) {
               return res;
            }

            sh += 7;
         }
      }
   }

   public static void writeINT(DataOutput record, int val) throws IOException {
      if (0 <= val && val < 192) {
         record.writeByte(val);
      } else {
         record.writeByte(192 + (val & 63));

         for(val >>>= 6; val >= 128; val >>>= 7) {
            record.writeByte(val & 127 | 128);
         }

         record.writeByte(val);
      }

   }

   public static String collapse(String methodSignature, final DictionaryLookup dictionaryLookup) {
      return processWithDictionary(methodSignature, new CoverageIOUtil.Consumer() {
         protected String consume(String type) {
            int dictionaryIndex = dictionaryLookup.getDictionaryIndex(type);
            return dictionaryIndex >= 0 ? String.valueOf(dictionaryIndex) : type;
         }
      });
   }

   static String processWithDictionary(String methodSignature, CoverageIOUtil.Consumer consumer) {
      Matcher matcher = TYPE_PATTERN.matcher(methodSignature);

      while(matcher.find()) {
         String s = matcher.group();
         if (s.startsWith("L") && s.endsWith(";")) {
            String type = s.substring(1, s.length() - 1);
            methodSignature = methodSignature.replace(type, consumer.consume(type));
         }
      }

      return methodSignature;
   }

   public static DataOutputStream openFile(File file) throws FileNotFoundException {
      return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
   }

   public static void close(DataOutputStream out) {
      if (out != null) {
         try {
            out.close();
         } catch (IOException var2) {
         }
      }

   }

   public abstract static class Consumer {
      protected abstract String consume(String var1);
   }
}

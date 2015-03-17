package by.muna.longedok.tools.track_adder;

import by.muna.longedok.LongedokConstants;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class TrackAdderMain {
    public static void main(String[] args) {
        String host, fileName;
        int port;

        switch (args.length) {
        case 1:
            host = "localhost";
            port = LongedokConstants.DEFAULT_PORT;
            fileName = args[0];
            break;
        case 2: {
            String[] parts = args[0].split(":", 2);
            if (parts.length != 2) {
                TrackAdderMain.printUsageAndExit(); return;
            }

            try {
                port = Integer.parseUnsignedInt(parts[1], 10);
            } catch (NumberFormatException ex) {
                TrackAdderMain.printUsageAndExit(); return;
            }
            host = parts[0];

            fileName = args[1];
        } break;
        default: TrackAdderMain.printUsageAndExit(); return;
        }

        URL manageUrl;
        try {
            manageUrl = new URL("http://" + host + ":" + port + "/_manage/0.0.1/tracks/new");
        } catch (MalformedURLException e) {
            System.err.println("Bad host");
            System.exit(1); return;
        }

        File file = new File(fileName);
        if (!file.isFile()) {
            System.err.println("No such file exists");
            System.exit(1); return;
        }

        int byteSize = (int) file.length();
        if (byteSize <= 0) {
            System.err.println("Invalid file size");
            System.exit(1); return;
        }

        Mp3File mp3File;

        try {
            mp3File = new Mp3File(file);
        } catch (IOException | UnsupportedTagException | InvalidDataException e) {
            e.printStackTrace();
            System.exit(1); return;
        }

        int bitrateKbps = mp3File.getBitrate(),
            duration = (int) mp3File.getLengthInSeconds();

        if (duration <= 0) {
            System.err.println("Invalid duration");
            System.exit(1); return;
        }

        String artist = null, album = null, title = null;
        int trackNo = 0;

        if (mp3File.hasId3v1Tag()) {
            ID3v1 tag = mp3File.getId3v1Tag();
            trackNo = TrackAdderMain.getFirstUint(tag.getTrack());
            artist = tag.getArtist();
            album = tag.getAlbum();
            title = tag.getTitle();
        }

        if (mp3File.hasId3v2Tag()) {
            ID3v2 tag = mp3File.getId3v2Tag();
            if (trackNo == 0) {
                trackNo = TrackAdderMain.getFirstUint(tag.getTrack());
            }
            artist = TrackAdderMain.takeGoodString(tag.getOriginalArtist(), artist);
            artist = TrackAdderMain.takeGoodString(tag.getAlbumArtist(), artist);
            album = TrackAdderMain.takeGoodString(tag.getAlbum(), album);
            title = TrackAdderMain.takeGoodString(tag.getTitle(), title);
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) manageUrl.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Charset", "utf-8");
            connection.setRequestProperty("Connection", "close");

            JSONObject json = new JSONObject();
            json.put("title", title);
            json.put("artist", album);
            json.put("album", artist);
            json.put("bitrate", bitrateKbps);
            json.put("duration", duration);
            json.put("trackNo", trackNo);
            json.put("filePath", file.getAbsolutePath());

            byte[] data = json.toString().getBytes(Charset.forName("UTF-8"));

            connection.setRequestProperty("Content-Length", Integer.toString(data.length));
            connection.setUseCaches(false);

            OutputStream os = connection.getOutputStream();
            os.write(data);
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                System.exit(0); return;
            }

            System.err.println(responseCode + " " + connection.getResponseMessage());

            try {
                InputStream is = connection.getErrorStream();
                Reader r = new InputStreamReader(is, Charset.forName("UTF-8"));

                char[] buffer = new char[1024];

                while (true) {
                    int readed = r.read(buffer);
                    if (readed == -1) break;

                    for (int i = 0; i < readed; i++) {
                        System.err.print(buffer[i]);
                    }
                }

                System.err.println();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static int getFirstUint(String s) {
        int result = 0;

        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if ('0' <= c && c <= '9') {
                result *= 10;
                result += c - '0';
            } else {
                return result;
            }
        }

        return result;
    }
    private static String takeGoodString(String newString, String oldString) {
        if (newString == null || newString.isEmpty()) return oldString;
        //if (oldString == null || oldString.isEmpty()) return newString;

        return newString;
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: track-adder [HOST:PORT] FILE");
        System.exit(1);
    }
}

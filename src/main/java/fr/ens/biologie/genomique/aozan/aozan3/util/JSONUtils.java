package fr.ens.biologie.genomique.aozan.aozan3.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * This class contains some utility methods for JSON serialization.
 * @author Laurent Jourdren
 * @since 3.0
 */
public final class JSONUtils {

  private static final TypeAdapter<Path> PATH_TYPE_ADAPTER =
      new TypeAdapter<Path>() {

        @Override
        public Path read(JsonReader in) throws IOException {

          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          String nextString = in.nextString();
          return "null".equals(nextString) ? null : Paths.get(nextString);
        }

        @Override
        public void write(JsonWriter out, Path value) throws IOException {
          out.value(value == null ? null : value.toString());
        }

      };

  /**
   * Create a new Gson object with all required adapters for serialization of
   * Aozan objects.
   * @return a new Gson object
   */
  public static Gson newGson() {

    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Path.class, PATH_TYPE_ADAPTER);
    return builder.create();
  }

  //
  // Private constructor
  //

  /**
   * Private constructor.
   */
  private JSONUtils() {
  }

}

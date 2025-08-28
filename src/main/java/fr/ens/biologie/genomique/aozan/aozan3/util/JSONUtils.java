package fr.ens.biologie.genomique.aozan.aozan3.util;

import java.lang.reflect.Type;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * This class contains some utility methods for JSON serialization.
 * @author Laurent Jourdren
 * @since 3.0
 */
public final class JSONUtils {

  public static class PathTypeAdapter
      implements JsonSerializer<Path>, JsonDeserializer<Path> {

    @Override
    public JsonElement serialize(final Path path, final Type typeOfSrc,
        final JsonSerializationContext context) {
      return new JsonPrimitive(path.toString());
    }

    @Override
    public Path deserialize(final JsonElement json, final Type typeOfT,
        final JsonDeserializationContext context) throws JsonParseException {
      return Path.of(json.getAsString());
    }
  }

  /**
   * Create a new Gson object with all required adapters for serialization of
   * Aozan objects.
   * @return a new Gson object
   */
  public static Gson newGson() {

    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeHierarchyAdapter(Path.class, new PathTypeAdapter());
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

class Monarch {
  Map generate(Map hierarchy, Iterable<Change> changes, String sourceToChange,
               Map<String, Map> data) {
    def ancestry = getAncestry(sourceToChange, hierarchy);
    // TODO: Also need to look at descendants
    def result = deepCopy(data);

    for (source in ancestry.reverse()) {
      def maybeChange = getChangeForSource(source, changes);

      if (!maybeChange.present) continue;

      def change = maybeChange.get();

      for (entry in change.set) {
        result[sourceToChange][entry.key] = entry.value
      }
      // TODO: Support removing nested keys (keys in a hash)
      for (key in change.remove) {
        result[sourceToChange].remove(key);
      }
    }

    return result;
  }

  Optional<Change> getChangeForSource(String source, Iterable<Change> changes) {
    def found = changes.findAll {it.source == source}
    if (found.empty) {
      return Optional.empty()
    }
    if (found.size() > 1) {
      throw new IllegalArgumentException(
          "Expected only one change with matching source in list of changes, but got: ${changes}")
    }
    return Optional.of(found.first())
  }

  List<String> getAncestry(String source, Map<String, ?> hierarchy, String parent = null) {
    for (entry in hierarchy) {
      def key = entry.key
      def value = entry.value

      if (value instanceof List && value.contains(source)) {
        return parent == null ? [source, key] : [source, key, parent]
      }

      if (value instanceof Map) {
        def a = getAncestry(source, value, key)
        return parent == null ? a : a.with { add(parent); return it }
      }

      if (value == source) {
        return [source, key] as List<String>
      }
    }

    return [];
  }

  private static Map deepCopy(Map<String, Map> data) {
    def copy = [:]
    data.each { k, v ->
      copy[k] = new HashMap<>(v)
    }
    return copy
  }
}

class Change {
  final String source
  final Map<String, ?> set
  final List remove

  Change(source, set = [:], remove = []) {
    this.source = source
    this.set = set
    this.remove = remove
  }

  static Change fromMap(Map map) {
    return new Change(map['source'], map['set'] ?: [:], map['remove'] ?: [])
  }
}

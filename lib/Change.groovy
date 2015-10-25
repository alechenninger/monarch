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

  @Override
  boolean equals(o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    Change change = (Change) o

    if (remove != change.remove) return false
    if (set != change.set) return false
    if (source != change.source) return false

    return true
  }

  @Override
  int hashCode() {
    int result
    result = (source != null ? source.hashCode() : 0)
    result = 31 * result + (set != null ? set.hashCode() : 0)
    result = 31 * result + (remove != null ? remove.hashCode() : 0)
    return result
  }

  @Override
  String toString() {
    return "Change{" +
        "source='" + source + '\'' +
        ", set=" + set +
        ", remove=" + remove +
        '}';
  }
}

package no.catenda.docs.c4;

import com.structurizr.component.Type;
import com.structurizr.component.naming.NamingStrategy;

/**
 * Naming strategy that uses the parent package + simple class name (e.g. "issue.IssueCommentResource").
 * This avoids collisions when two classes in different packages share the same simple name,
 * while keeping names short enough for readable C4 diagrams.
 */
public class ParentPackageNamingStrategy implements NamingStrategy {

  @Override
  public String nameOf(Type type) {
    String packageName = type.getPackageName();
    String parentPackage = packageName.contains(".")
        ? packageName.substring(packageName.lastIndexOf('.') + 1)
        : packageName;
    return parentPackage + "." + type.getName();
  }
}
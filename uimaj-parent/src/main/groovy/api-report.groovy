  def it = jApiClasses.iterator()
  while (it.hasNext()) {
    def jApiClass = it.next()
    def fqn = jApiClass.getFullyQualifiedName()
    if (fqn.contains("impl")) {
      it.remove()
    }
  }  
  return jApiClasses
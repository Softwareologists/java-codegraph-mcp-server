{
  "name": "CodeGraph MCP",
  "version": "0.1.0",
  "capabilities": [
    { "name": "findCallers", "params": ["className"] },
    { "name": "findImplementations", "params": ["interfaceName"] },
    { "name": "findSubclasses", "params": ["className", "depth"] },
    { "name": "findDependencies", "params": ["className", "depth"] },
    { "name": "findPathBetweenClasses", "params": ["fromClass", "toClass", "maxDepth"] },
    { "name": "findMethodsCallingMethod", "params": ["className", "methodSignature", "limit"] },
    { "name": "findBeansWithAnnotation", "params": ["annotation"] },
    { "name": "searchByAnnotation", "params": ["annotation", "targetType"] },
    { "name": "findControllersUsingService", "params": ["serviceClassName"] },
    { "name": "getPackageHierarchy", "params": ["rootPackage", "depth"] }
  ]
}

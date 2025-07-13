{
  "name": "CodeGraph MCP",
  "version": "0.1.0",
  "capabilities": [
    { "name": "findCallers", "params": ["className"], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "findImplementations", "params": ["interfaceName"], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "findSubclasses", "params": ["className", "depth"], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "findDependencies", "params": ["className", "depth"], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "findPathBetweenClasses", "params": ["fromClass", "toClass", "maxDepth"] },
    { "name": "findMethodsCallingMethod", "params": ["className", "methodSignature", "limit"], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "findBeansWithAnnotation", "params": ["annotation"], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "searchByAnnotation", "params": ["annotation", "targetType"], "defaults": {"limit": 100, "page": 1, "pageSize": 50, "targetType": "class"} },
    { "name": "findHttpEndpoints", "params": ["basePath", "httpMethod"], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "findControllersUsingService", "params": ["serviceClassName"], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "findEventListeners", "params": ["eventType"], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "findScheduledTasks", "params": [], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "findConfigPropertyUsage", "params": ["propertyKey"], "defaults": {"limit": 100, "page": 1, "pageSize": 50} },
    { "name": "getPackageHierarchy", "params": ["rootPackage", "depth"] },
    { "name": "getGraphStatistics", "params": ["topN"], "defaults": {"topN": 10} },
    { "name": "exportGraph", "params": ["format", "outputPath"] }
  ]
}

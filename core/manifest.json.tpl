{
  "name": "CodeGraph MCP",
  "version": "0.1.0",
  "capabilities": [
    { "name": "findCallers", "params": ["className"] },
    { "name": "findImplementations", "params": ["interfaceName"] },
    { "name": "findSubclasses", "params": ["className", "depth"] },
    { "name": "findDependencies", "params": ["className", "depth"] },
    { "name": "findMethodsCallingMethod", "params": ["className", "methodSignature", "limit"] }
  ]
}

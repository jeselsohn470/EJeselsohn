Document Store (Data Structures  Project, Spring 2020)

Project description

• Developed a search engine that stores text or pdf files in memory, utilizing several built-from-scratch data structures, including Hash-Table, Stack, Trie, Heap, and B-Tree.

• Programmed to keep an O(1) dictionary lookup which maps words/prefixes to all documents containing that word/prefix.

• Using PDF Box library, the application can return any text document as a pdf or vice versa.

• User can define a limit of how many documents can be stored in memory.If the limit is reached, the least recently used document is stored on disk in JSON format.

• System tracks all actions chronologically, allowing for undo capability.
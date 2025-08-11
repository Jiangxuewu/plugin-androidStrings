
import { GoogleGenerativeAI, HarmCategory, HarmBlockThreshold } from '@google/generative-ai';
import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

// --- Tool Definitions ---
// These are the functions that Gemini can call.

const readFile = (filePath) => {
  console.log(`Tool: Reading file from ${filePath}`);
  try {
    return fs.readFileSync(filePath, 'utf-8');
  } catch (e) {
    return `Error reading file: ${e.message}`;
  }
};

const writeFile = ({ filePath, content }) => {
  console.log(`Tool: Writing ${content.length} chars to ${filePath}`);
  try {
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    fs.writeFileSync(filePath, content, 'utf-8');
    return `Successfully wrote to ${filePath}`;
  } catch (e) {
    return `Error writing file: ${e.message}`;
  }
};

const listFiles = (directoryPath) => {
  console.log(`Tool: Listing files in ${directoryPath}`);
  try {
    // Using find is more robust than ls for recursive listing
    return execSync(`find ${directoryPath} -type f`).toString();
  } catch (e) {
    return `Error listing files: ${e.message}`;
  }
};

const runShellCommand = (command) => {
  console.log(`Tool: Running shell command: ${command}`);
  try {
    const output = execSync(command, { encoding: 'utf-8' });
    return output;
  } catch (e) {
    // Return stdout and stderr for context on failure
    return `Error executing command: ${e.message}\nSTDOUT: ${e.stdout}\nSTDERR: ${e.stderr}`;
  }
};

const availableTools = {
  readFile,
  writeFile,
  listFiles,
  runShellCommand,
};

// --- Main Agent Logic ---

async function main() {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY environment variable not set.');
  }

  const genAI = new GoogleGenerativeAI(apiKey);

  const tools = [
    {
      functionDeclarations: [
        { name: 'readFile', description: 'Read the content of a file at a given path.', parameters: { type: 'string', description: 'The path to the file.' } },
        { name: 'writeFile', description: 'Write content to a file at a given path. Creates directories if they don\'t exist.', parameters: { type: 'object', properties: { filePath: { type: 'string' }, content: { type: 'string' } }, required: ['filePath', 'content'] } },
        { name: 'listFiles', description: 'List all files recursively in a given directory path.', parameters: { type: 'string', description: 'The path to the directory.' } },
        { name: 'runShellCommand', description: 'Execute a shell command. Important for testing (./gradlew test), building (./gradlew build), etc.', parameters: { type: 'string', description: 'The command to execute.' } },
      ],
    },
  ];

  const model = genAI.getGenerativeModel({ model: 'gemini-1.5-pro-latest', tools });

  const issueBody = process.env.ISSUE_BODY || '';
  const issueNumber = process.env.ISSUE_NUMBER || '';

  const initialPrompt = `
    You are Gemini-CLI, an expert software engineer AI assistant.
    Your task is to solve the GitHub issue #${issueNumber}.
    The user has requested a new feature or bug fix. Your goal is to implement it, test it, and ensure the code is ready to be committed.

    **CONTEXT:**
    This is a Java project built with Gradle. 
    - Use './gradlew build' to build the project.
    - Use './gradlew test' to run tests.
    - The main source code is in 'src/main/java'.
    - The test source code is in 'src/test/java'.

    **PLAN:**
    1. First, understand the project structure. Use 'listFiles' on 'src' to see the existing files.
    2. Based on the issue description, formulate a plan. Think step-by-step about which files to read, modify, or create.
    3. Implement the changes using 'readFile' and 'writeFile'.
    4. If you add new logic, you MUST add or update tests. Use 'readFile' to understand existing tests for context.
    5. After making changes, ALWAYS run './gradlew test' to ensure you haven\'t broken anything.
    6. If tests fail, debug the issue and fix it. Repeat until all tests pass.
    7. Once all tests pass, respond with a final message: 
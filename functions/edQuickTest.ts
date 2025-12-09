// functions/edQuickTest.ts

import path from "node:path";
import dotenv from "dotenv";

dotenv.config({
  // .env is located at the functions root (same folder as edQuickTest.ts when using ts-node)
  path: path.join(__dirname, ".env"),
});

import { EdDiscussionClient } from "./src/connectors/ed/EdDiscussionClient";
import {
  parseEdSearchQuery,
  buildEdSearchRequest,
  normalizeEdThreads,
} from "./src/edSearchDomain";

async function main() {
  // ⚠️ Mets ton vrai token ED ici pour le test.
  // N'OUBLIE PAS de l'enlever avant de commit.
  const token = "met ton token ed ici";
  const baseUrl = "https://eu.edstem.org/api";

  const client = new EdDiscussionClient(baseUrl, token);

  // 1) User + cours
  const user = await client.getUser();
  console.log("Logged in as:", user.user.email);

  const courses = user.courses.map((c) => c.course);
  console.log(
    "Courses:",
    courses.map((c) => `${c.id} - ${c.code} - ${c.name}`)
  );

  // 2) Quelques requêtes NL qu'on veut tester
  // ⚠️ Important : prends un cours avec un code du style COM-202, COM-300, CS-202, etc.
  // (le parser actuel cherche ce pattern: [A-Z]{2,4}-[0-9]{2,3})
  const sampleQueries = [
    "check on the CompSec ED what people say about the 3th assignment",
  ];

  for (const q of sampleQueries) {
    console.log("\n====================================");
    console.log("USER QUERY:", q);

    // 2a) Parsing NL → EdSearchParsedQuery
    const parsed = parseEdSearchQuery(q);
    console.log("Parsed query:", parsed);

    // 2b) Résolution du cours + construction des options ED
    const req = await buildEdSearchRequest(client, parsed, courses);

    // Si c'est une erreur structurée du brain (INVALID_QUERY, etc.)
    if ("type" in req) {
      console.log("Brain error:", req);
      continue;
    }

    console.log(
      "Resolved course:",
      req.resolvedCourse.id,
      req.resolvedCourse.code,
      "-",
      req.resolvedCourse.name
    );
    console.log("Fetch options sent to ED:", req.fetchOptions);

    // 2c) Appel réel à ED pour chercher les threads
    const threads = await client.fetchThreads(req.fetchOptions);
    console.log(`Fetched ${threads.length} raw threads from ED`);

    // 2d) Normalisation pour le frontend
    const normalized = normalizeEdThreads(threads, req.resolvedCourse);
    console.log("First normalized post (if any):");
    console.dir(normalized[0], { depth: null });
  }
}

main().catch((e) => {
  console.error("Error in edQuickTest:", e);
});

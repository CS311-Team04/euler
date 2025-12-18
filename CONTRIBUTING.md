# Contributing to Euler

First off, thank you for considering contributing to Euler! It's people like you that make Euler such a great tool for the EPFL community.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Workflow](#development-workflow)
- [Style Guidelines](#style-guidelines)
- [Testing Requirements](#testing-requirements)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)

## Code of Conduct

This project and everyone participating in it is governed by our commitment to creating a welcoming and inclusive environment. By participating, you are expected to:

- Use welcoming and inclusive language
- Be respectful of differing viewpoints and experiences
- Gracefully accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

Before you begin, make sure you have:

- **Android Studio** Jellyfish (2024.2.1) or newer
- **JDK 17** or higher
- **Git** for version control
- **Node.js 20** (for backend functions development)
- A **GitHub account**
- **Firebase account** for testing (with Firestore and Authentication enabled)

### Setting Up Your Development Environment

1. **Fork the repository on GitHub**

2. **Clone your fork locally:**
   ```bash
   git clone https://github.com/YOUR-USERNAME/euler.git
   cd euler
   ```

3. **Add the upstream repository:**
   ```bash
   git remote add upstream https://github.com/CS311-Team04/euler.git
   ```

4. **Configure Firebase** (see main README.md for details):
   - Add your `google-services.json` file to the `app/` directory
   - Ensure Firebase project has Firestore and Authentication enabled

5. **Configure Backend (Optional - for local development):**
   ```bash
   cd functions
   npm install
   # Configure environment variables in .env file
   npm run build
   ```

6. **Build the project to ensure everything works:**
   ```bash
   ./gradlew assembleDebug
   ```

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates.

When creating a bug report, include:

- **Clear title**: Descriptive and specific
- **Steps to reproduce**: Detailed step-by-step instructions
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Screenshots/Videos**: Visual evidence if applicable
- **Environment**:
  - Android version
  - Device model
  - App version
  - Network connectivity status
- **Additional context**: Any other relevant information (connector status, authentication state, etc.)

### Suggesting Features

Feature suggestions are welcome! When suggesting a feature:

- Use a **clear title**: Be specific about the feature
- Provide **detailed description**: Explain the feature and its benefits
- Describe **use cases**: When and why would this be useful for EPFL students?
- Include **mockups/examples**: Visual aids help (optional)
- Consider **alternatives**: Are there other ways to achieve this?
- Check if it aligns with the project's goal of unifying EPFL's digital ecosystem

### Contributing Code

We welcome code contributions! Here are areas where you can help:

- **Bug fixes**: Check issues labeled `bug`
- **New features**: Check issues labeled `enhancement`
- **Documentation**: Improve existing docs or add new ones
- **Tests**: Increase test coverage
- **Performance**: Optimize existing code
- **Refactoring**: Improve code quality
- **Connectors**: Enhance Moodle, Ed Discussion, or EPFL Campus integrations
- **Backend functions**: Improve RAG, intent detection, or connector services

## Development Workflow

### Branch Strategy

We use the following branch naming conventions:

- `feature/description` - New features
- `fix/description` - Bug fixes
- `refactor/description` - Code refactoring
- `docs/description` - Documentation updates
- `test/description` - Test additions/improvements
- `backend/description` - Backend/Firebase Functions changes

### Creating a Feature Branch

```bash
# Update your local main branch
git checkout main
git pull upstream main

# Create and checkout a new branch
git checkout -b feature/your-feature-name
```

### Making Changes

1. **Write your code**: Implement your changes
2. **Follow style guidelines**: Ensure code follows project conventions
3. **Add tests**: Write tests for new functionality
4. **Update documentation**: Update relevant docs if needed
5. **Run tests locally**: Ensure all tests pass

```bash
# Format code
./gradlew ktfmtFormat

# Check formatting
./gradlew ktfmtCheck

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest

# Check code coverage
./gradlew jacocoTestReport

# For backend functions
cd functions
npm test
```

## Style Guidelines

### Kotlin Code Style

We use **ktfmt** for code formatting. Run before committing:

```bash
./gradlew ktfmtFormat
```

### General Guidelines

- **Naming**: Use clear, descriptive names for variables, functions, and classes
- **Comments**: Write comments for complex logic, not obvious code
- **Functions**: Keep functions small and focused on a single task
- **Error Handling**: Handle errors gracefully with proper messages
- **Null Safety**: Leverage Kotlin's null safety features
- **Compose**: Follow Jetpack Compose best practices and Material Design 3 guidelines
- **Coroutines**: Use appropriate coroutine dispatchers and handle cancellation properly
- **State Management**: Use StateFlow and Flow for reactive state management

### File Organization

```
app/src/main/java/com/android/sample/
├── auth/                    # Authentication logic
├── home/                    # Main chat screen
├── Chat/                    # Chat UI models
├── conversations/           # Conversation management
├── epfl/                    # EPFL Campus integrations
├── llm/                     # LLM client abstraction
├── navigation/              # Navigation graph
├── network/                 # Network monitoring
├── onboarding/              # Onboarding screens
├── profile/                 # User profile
├── settings/                # Settings and connectors
├── speech/                  # Speech recognition/synthesis
├── splash/                  # Splash screen
├── ui/                      # Reusable UI components
│   ├── components/          # Shared composables
│   └── theme/              # Theme and styling
└── VoiceChat/              # Voice chat interface
```

### Backend (TypeScript) Guidelines

- Follow TypeScript best practices
- Use async/await for asynchronous operations
- Handle errors with try-catch blocks
- Document function parameters and return types
- Use Firebase Functions v1 API
- Follow the existing connector service patterns

## Testing Requirements

### Test Coverage

- **Minimum coverage**: 80% for new code
- **Unit tests**: Required for all business logic
- **Integration tests**: Required for complex features (connectors, RAG)
- **UI tests**: Recommended for critical user flows (chat, authentication)

### Writing Tests

- Use **descriptive names**: Test names should explain what they test
- **Arrange-Act-Assert**: Follow the AAA pattern
- **Mock dependencies**: Use MockK for Kotlin code, Jest mocks for TypeScript
- **Test edge cases**: Don't just test the happy path
- **Test offline scenarios**: Consider network failures and offline mode

### Example (Kotlin)

```kotlin
@Test
fun `sendMessage should update UI state and call LLM client`() = runTest {
    // Arrange
    val message = "What's my schedule?"
    val mockLlmClient = mockk<LlmClient>()
    coEvery { mockLlmClient.sendMessage(any(), any()) } returns BotReply("Your schedule...")
    
    // Act
    viewModel.sendMessage(message)
    
    // Assert
    coVerify { mockLlmClient.sendMessage(message, any()) }
    assertTrue(viewModel.uiState.value.messages.any { it.content == message })
}
```

### Example (TypeScript)

```typescript
describe('answerWithRagCore', () => {
  it('should return RAG answer with schedule context when question is schedule-related', async () => {
    // Arrange
    const question = "What's my schedule tomorrow?";
    const uid = "test-user-123";
    
    // Act
    const result = await answerWithRagCore({ question, uid });
    
    // Assert
    expect(result.answer).toContain("schedule");
    expect(result.sources).toBeDefined();
  });
});
```

## Commit Message Guidelines

We follow the **Conventional Commits** specification:

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `backend`: Backend/Firebase Functions changes

### Examples

```
feat(chat): add voice input support

Implemented speech-to-text integration with Android SpeechRecognizer.
Added microphone permission handling and error states.

Closes #123
```

```
fix(moodle): resolve file download failure on Android 13

Updated file download logic to use MediaStore API for Android 13+.
Added proper permission checks and file type handling.

Fixes #456
```

```
backend(rag): improve Qdrant search performance

Optimized hybrid search query with better sparse vector weighting.
Reduced response time by 30% for complex queries.

Related to #789
```

## Pull Request Process

### Before Submitting

- [ ] Code follows style guidelines
- [ ] All tests pass locally
- [ ] New tests added for new functionality
- [ ] Documentation updated if needed
- [ ] No merge conflicts with main branch
- [ ] Code coverage maintained or improved
- [ ] Backend functions tested (if applicable)
- [ ] Firebase emulator tested (if backend changes)

### Submitting a Pull Request

1. **Push your changes to your fork:**
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create a Pull Request on GitHub:**
   - Use a clear, descriptive title
   - Fill out the PR template completely
   - Link related issues (e.g., "Closes #123")
   - Add screenshots/videos for UI changes
   - Request review from maintainers

3. **Address feedback:**
   - Respond to review comments
   - Make requested changes
   - Push updates to the same branch
   - Re-request review when ready

4. **After approval:**
   - Maintainer will merge your PR
   - Delete your feature branch after merge
   - Update your local main branch:
     ```bash
     git checkout main
     git pull upstream main
     ```

### PR Review Criteria

Reviewers will check:

- **Functionality**: Does it work as intended?
- **Code Quality**: Is the code clean and maintainable?
- **Tests**: Are there sufficient tests?
- **Documentation**: Is documentation updated?
- **Performance**: Does it impact app performance?
- **Security**: Are there any security concerns? (especially for connectors and authentication)
- **Offline Support**: Does it handle offline scenarios gracefully?
- **Localization**: Are new strings properly localized?

## Getting Help

If you need help at any point:

- **Documentation**: Check the main README.md and GitHub Wiki
- **Discussions**: Post in GitHub Discussions
- **Issues**: Comment on relevant issues
- **Ask the team**: Reach out to maintainers

## Recognition

Contributors will be recognized in:

- Pull request acknowledgments
- Release notes
- Project documentation

---

Thank you for contributing to Euler! 🎉


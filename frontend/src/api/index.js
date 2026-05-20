// Axios instance with interceptors
export { apiClient, unwrapApiData, extractApiMessage } from './request.js';

// Auth API
export { login, register, logout, loginByCode, sendCode, verifyCode, setPassword, getCurrentUser } from './auth.js';

// Resume CRUD API
export {
  getResumes,
  getResume,
  createResume,
  updateResume,
  deleteResume,
  addSection,
  updateSection,
  deleteSection,
  reorderSections,
  uploadResumeAvatar,
  saveResumeVersion,
  getResumeVersions,
  getResumeVersionDetail,
  restoreResumeVersion,
} from './resumes.js';

export {
  createAgentSession,
  listAgentSessions,
  getAgentSessionDetail,
  updateAgentSession,
  deleteAgentSession,
  listAgentMessages,
  createAgentMessage,
} from './agentSessions.js';

export {
  approveAgentRun,
  cancelAgentRun,
  continueAgentRun,
  createAgentRun,
  enqueueContinueAgentRun,
  getAgentRun,
  getAgentRunEvents,
  setAgentMockEnabled,
  shouldUseAgentMock,
  streamAgentRun,
  streamAgentRunEvents,
} from './agent.js';

export {
  getJavaHealth,
} from './health.js';

export {
  finishInterviewRun,
  getInterviewBoard,
  getInterviewQuestionRounds,
  startInterviewRun,
  submitInterviewAnswer,
} from './interview.js';

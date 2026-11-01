// Axios instance with interceptors
export { apiClient, unwrapApiData, extractApiMessage } from './request.js';

// 本地 API 会话
export { getCurrentUser } from './auth.js';

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
  listAgentSessionRuns,
  updateAgentSession,
  deleteAgentSession,
  listAgentMessages,
  createAgentMessage,
} from './agentSessions.js';

export {
  approveAgentRun,
  cancelAgentRun,
  continueAgentRun,
  createClientRequestId,
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
  finishInterviewRun,
  getInterviewBoard,
  getInterviewQuestionRounds,
  startInterviewRun,
  submitInterviewAnswer,
} from './interview.js';

import { apiClient, unwrapApiData } from './request.js';

export async function getResumes() {
  const res = await apiClient.get('/api/resumes');
  return unwrapApiData(res) || [];
}

export async function getResume(id) {
  const res = await apiClient.get(`/api/resumes/${id}`);
  return unwrapApiData(res);
}

export async function createResume(data) {
  const res = await apiClient.post('/api/resumes', data);
  return unwrapApiData(res);
}

export async function updateResume(id, data) {
  const res = await apiClient.put(`/api/resumes/${id}`, data);
  return unwrapApiData(res);
}

export async function deleteResume(id) {
  const res = await apiClient.delete(`/api/resumes/${id}`);
  return unwrapApiData(res);
}

export async function addSection(resumeId, data) {
  const payload = {
    ...data,
    contentJson:
      typeof data?.contentJson === 'string'
        ? data.contentJson
        : JSON.stringify(data?.contentJson ?? {}),
  };

  const res = await apiClient.post(`/api/resumes/${resumeId}/sections`, payload);
  return unwrapApiData(res);
}

export async function updateSection(resumeId, sectionId, data) {
  const res = await apiClient.put(`/api/resumes/${resumeId}/sections/${sectionId}`, data);
  return unwrapApiData(res);
}

export async function deleteSection(resumeId, sectionId) {
  const res = await apiClient.delete(`/api/resumes/${resumeId}/sections/${sectionId}`);
  return unwrapApiData(res);
}

export async function reorderSections(resumeId, sectionIds) {
  const res = await apiClient.put(`/api/resumes/${resumeId}/sections/reorder`, { sectionIds });
  return unwrapApiData(res);
}

export async function uploadResumeAvatar(resumeId, file) {
  const formData = new FormData();
  formData.append('file', file);

  const res = await apiClient.post(`/api/resumes/${resumeId}/avatar`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return unwrapApiData(res);
}

export async function saveResumeVersion(resumeId, data) {
  const res = await apiClient.post(`/api/resumes/${resumeId}/versions`, data);
  return unwrapApiData(res);
}

export async function getResumeVersions(resumeId) {
  const res = await apiClient.get(`/api/resumes/${resumeId}/versions`);
  return unwrapApiData(res) || [];
}

export async function getResumeVersionDetail(resumeId, versionId) {
  const res = await apiClient.get(`/api/resumes/${resumeId}/versions/${versionId}`);
  return unwrapApiData(res);
}

export async function restoreResumeVersion(resumeId, versionId) {
  const res = await apiClient.post(`/api/resumes/${resumeId}/versions/${versionId}/restore`);
  return unwrapApiData(res);
}

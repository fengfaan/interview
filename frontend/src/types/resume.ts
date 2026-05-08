export interface AnalyzeRequest {
  jobDescription: string
  resume: string
}

export interface Dimension {
  name: string
  score: number
  reason: string
}

export interface Suggestion {
  id: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  title: string
  reason: string
  sourceText: string
}

export interface AnalyzeResponse {
  score: number
  dimensions: Dimension[]
  suggestions: Suggestion[]
}

export interface RewriteStreamRequest {
  jobDescription: string
  resume: string
  suggestion: {
    id: string
    title: string
    sourceText: string
  }
}

export interface StructureAnalysisRequest {
  resume: string;
}

export interface ModuleCheck {
  name: string;
  status: 'pass' | 'warn' | 'fail';
  detail: string;
}

export interface StructuralIssue {
  severity: 'critical' | 'warning' | 'info';
  description: string;
  suggestion: string;
}

export interface StructureAnalysisResponse {
  structureScore: number;
  moduleChecks: ModuleCheck[];
  issues: StructuralIssue[];
  summary: string;
}

export interface PolishStreamRequest {
  sourceText: string;
  jobDescription?: string;
}

export interface ImportFileResponse {
  text: string;
  fileName: string;
  pageCount: number;
  warning?: string;
}

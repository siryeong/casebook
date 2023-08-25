import React, { useState, useEffect, useMemo } from 'react';
import { Block, BlockRow, Button, EmptyContent, Label, Page, PageContent, PageTitle, TestcaseSelectorSummary, Text, Title } from '@/components';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router';
import ProjectService from '@/services/ProjectService';
import ReleaseService from '@/services/ReleaseService';
import testcaseUtil from '@/utils/testcaseUtil';

const LABEL_MIN_WIDTH = '120px';

function ReleaseInfoPage() {
  const { t } = useTranslation();
  const { spaceCode, projectId, releaseId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState(null);
  const [release, setRelease] = useState({});

  useEffect(() => {
    ProjectService.selectProjectInfo(spaceCode, projectId, info => {
      setProject(info);
    });

    ReleaseService.selectRelease(spaceCode, projectId, releaseId, info => {
      setRelease(info);
    });
  }, [spaceCode, projectId]);

  const selectedTestcaseGroup = useMemo(
    () =>
      testcaseUtil.getSelectionFromTestcaseGroups(
        (project?.testcaseGroups ?? []).map(group => ({ ...group, testcases: group.testcases.filter(testcase => testcase.projectReleaseId === +releaseId) })),
      ),
    [project?.testcaseGroups, releaseId],
  );
  const selectedTestcaseGroupSummary = useMemo(() => testcaseUtil.getSelectedTestcaseGroupSummary(selectedTestcaseGroup, project?.testcaseGroups), [selectedTestcaseGroup, project?.testcaseGroups]);

  return (
    <Page>
      <PageTitle
        breadcrumbs={[
          { to: '/', text: t('HOME') },
          {
            to: '/',
            text: t('스페이스 목록'),
          },
          {
            to: `/spaces/${spaceCode}/info`,
            text: spaceCode,
          },
          {
            to: `/spaces/${spaceCode}/projects`,
            text: t('프로젝트 목록'),
          },
          {
            to: `/spaces/${spaceCode}/projects/${projectId}`,
            text: project?.name,
          },
          {
            to: `/spaces/${spaceCode}/projects/${projectId}/releases`,
            text: t('릴리즈 목록'),
          },
          {
            to: `/spaces/${spaceCode}/projects/${projectId}/releases/${releaseId}`,
            text: release?.name,
          },
        ]}
        control={<Button onClick={() => navigate('edit')}>{t('편집')}</Button>}
      >
        {t('새 릴리즈')}
      </PageTitle>
      <PageContent>
        <Title border={false} marginBottom={false}>
          {t('릴리즈 정보')}
        </Title>
        <Block>
          <BlockRow>
            <Label minWidth={LABEL_MIN_WIDTH}>{t('프로젝트')}</Label>
            <Text>{project?.name}</Text>
          </BlockRow>
          <BlockRow>
            <Label minWidth={LABEL_MIN_WIDTH} required>
              {t('이름')}
            </Label>
            <Text>{release.name}</Text>
          </BlockRow>
          <BlockRow>
            <Label minWidth={LABEL_MIN_WIDTH}>{t('설명')}</Label>
            <Text>{release.description}</Text>
          </BlockRow>
          <BlockRow>
            <Label minWidth={LABEL_MIN_WIDTH}>{t('테스트케이스')}</Label>
            {release?.testcases?.length < 1 && <EmptyContent border>{t('선택된 테스트케이스가 없습니다.')}</EmptyContent>}
            {release?.testcases?.length > 0 && <TestcaseSelectorSummary selectedTestcaseGroupSummary={selectedTestcaseGroupSummary} />}
          </BlockRow>
        </Block>
      </PageContent>
    </Page>
  );
}

export default ReleaseInfoPage;

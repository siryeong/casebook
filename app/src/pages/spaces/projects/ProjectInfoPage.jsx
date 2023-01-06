import React, { useEffect, useState } from 'react';
import { Block, Button, Card, CardContent, CardHeader, Label, Liner, Page, PageButtons, PageContent, PageTitle, Text, Title } from '@/components';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom';
import { useParams } from 'react-router';
import BlockRow from '@/components/BlockRow/BlockRow';
import './ProjectInfoPage.scss';
import ProjectService from '@/services/ProjectService';
import TestcaseTemplateEditorPopup from '@/pages/spaces/projects/TestcaseTemplateEditorPopup/TestcaseTemplateEditorPopup';
import MemberCardManager from '@/components/MemberManager/MemberCardManager';
import dialogUtil from '@/utils/dialogUtil';
import { MESSAGE_CATEGORY } from '@/constants/constants';

function ProjectInfoPage() {
  const { t } = useTranslation();
  const { spaceCode, projectId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState(null);
  const [templateViewerPopupInfo, setTemplateViewerPopupInfo] = useState({
    opened: false,
    testcaseTemplate: null,
  });

  useEffect(() => {
    window.scrollTo(0, 0);
    ProjectService.selectProjectInfo(spaceCode, projectId, info => {
      setProject(info);
    });
  }, [projectId]);

  const onDelete = () => {
    dialogUtil.setConfirm(
      MESSAGE_CATEGORY.WARNING,
      t('프로젝트 삭제'),
      <div>{t(`"${project.name}" 프로젝트 및 프로젝트에 포함된 모든 정보가 삭제됩니다. 삭제하시겠습니까?`)}</div>,
      () => {
        ProjectService.deleteProject(spaceCode, project, () => {
          navigate(`/spaces/${spaceCode}/projects`);
        });
      },
      null,
      t('삭제'),
    );
  };

  const onWithdraw = () => {
    dialogUtil.setConfirm(
      MESSAGE_CATEGORY.WARNING,
      t('프로젝트 탈퇴'),
      <div>{t(`프로젝트를 탈퇴하면, "${project.name}" 프로젝트에 더 이상 접근할 수 없습니다. 탈퇴하시겠습니까?`)}</div>,
      () => {
        ProjectService.withdrawProject(spaceCode, project, () => {
          navigate(`/spaces/${spaceCode}/projects`);
        });
      },
      null,
      t('탈퇴'),
    );
  };

  return (
    <>
      <Page className="project-info-page-wrapper">
        <PageTitle links={project?.admin ? [<Link to={`/spaces/${spaceCode}/projects/${project.id}/edit`}>{t('프로젝트 변경')}</Link>] : null}>{t('프로젝트')}</PageTitle>
        <PageContent>
          <Title>{t('프로젝트 정보')}</Title>
          <Block>
            <BlockRow>
              <Label>{t('스페이스')}</Label>
              <Text>{project?.spaceName}</Text>
            </BlockRow>
            <BlockRow>
              <Label>{t('이름')}</Label>
              <Text>{project?.name}</Text>
            </BlockRow>
            <BlockRow>
              <Label>{t('설명')}</Label>
              <Text>{project?.description}</Text>
            </BlockRow>
            <BlockRow>
              <Label>{t('활성화')}</Label>
              <Text>{project?.activated ? 'Y' : 'N'}</Text>
            </BlockRow>
            <BlockRow>
              <Label>{t('토큰')}</Label>
              <Text>{project?.token}</Text>
            </BlockRow>
          </Block>
          <Title>테스트케이스 템플릿</Title>
          <Block>
            <ul className="template-list">
              {project?.testcaseTemplates?.map(testcaseTemplate => {
                return (
                  <li key={testcaseTemplate.id}>
                    <Card border className="testcase-template" point>
                      <CardHeader className="name">
                        <div>
                          <span
                            onClick={() => {
                              setTemplateViewerPopupInfo({
                                opened: true,
                                testcaseTemplate,
                              });
                            }}
                          >
                            {testcaseTemplate.name}
                          </span>
                        </div>
                        {testcaseTemplate.defaultTemplate && (
                          <div className="default">
                            <span>DEFAULT</span>
                          </div>
                        )}
                      </CardHeader>
                      <CardContent className="testcase-template-content">
                        <div className="item-count">
                          <span className="count">
                            <span>{testcaseTemplate.testcaseTemplateItems?.length}</span>
                          </span>
                          <span className="count-label">아이템</span>
                        </div>
                      </CardContent>
                    </Card>
                  </li>
                );
              })}
            </ul>
          </Block>
          <Title>프로젝트 사용자</Title>
          <Block>
            <MemberCardManager className="member-manager" users={project?.users} />
          </Block>
          <Title>{t('프로젝트 관리')}</Title>
          <Block className="space-control">
            <Button color="warning" onClick={onWithdraw}>
              {t('프로젝트 탈퇴')}
            </Button>
            {project?.admin && (
              <>
                <Liner width="1px" height="10px" display="inline-block" color="gray" margin="0 1rem" />
                <Button color="danger" onClick={onDelete}>
                  {t('프로젝트 삭제')}
                </Button>
              </>
            )}
          </Block>
          <PageButtons
            outline
            onBack={() => {
              navigate(-1);
            }}
            onEdit={
              project?.admin
                ? () => {
                    navigate(`/spaces/${spaceCode}/projects/${project.id}/edit`);
                  }
                : null
            }
            onCancelIcon=""
          />
        </PageContent>
      </Page>
      <TestcaseTemplateEditorPopup
        editor={false}
        opened={templateViewerPopupInfo.opened}
        testcaseTemplate={templateViewerPopupInfo.testcaseTemplate}
        onClose={() => {
          setTemplateViewerPopupInfo({
            opened: false,
          });
        }}
      />
    </>
  );
}

ProjectInfoPage.defaultProps = {};

ProjectInfoPage.propTypes = {};

export default ProjectInfoPage;

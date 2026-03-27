@echo off
set BASE=%~dp0src\main\java\com\trolmastercard\sexmod

echo Fixing misplaced and duplicate files...

REM -- Delete stale root copies that have correct versions in subfolders --
if exist "%BASE%\ColoredNpcArmRenderer.java" del "%BASE%\ColoredNpcArmRenderer.java"
if exist "%BASE%\ColoredNpcHandRenderer.java" del "%BASE%\ColoredNpcHandRenderer.java"
if exist "%BASE%\CummyParticleRenderer.java" del "%BASE%\CummyParticleRenderer.java"
if exist "%BASE%\CustomizeNpcPacket.java" del "%BASE%\CustomizeNpcPacket.java"
if exist "%BASE%\ElEntityRenderer.java" del "%BASE%\ElEntityRenderer.java"
if exist "%BASE%\EllieNpcRenderer.java" del "%BASE%\EllieNpcRenderer.java"
if exist "%BASE%\ExNpcRenderer.java" del "%BASE%\ExNpcRenderer.java"
if exist "%BASE%\FigureNpcRenderer.java" del "%BASE%\FigureNpcRenderer.java"
if exist "%BASE%\FishingLineRenderer.java" del "%BASE%\FishingLineRenderer.java"
if exist "%BASE%\FishingLineSegmentRenderer.java" del "%BASE%\FishingLineSegmentRenderer.java"
if exist "%BASE%\FzEntityRenderer.java" del "%BASE%\FzEntityRenderer.java"
if exist "%BASE%\GalathAttackState.java" del "%BASE%\GalathAttackState.java"
if exist "%BASE%\GalathFlightController.java" del "%BASE%\GalathFlightController.java"
if exist "%BASE%\GoblinBodyRenderer.java" del "%BASE%\GoblinBodyRenderer.java"
if exist "%BASE%\GoblinEntityRenderer.java" del "%BASE%\GoblinEntityRenderer.java"
if exist "%BASE%\GoblinHandRenderer.java" del "%BASE%\GoblinHandRenderer.java"
if exist "%BASE%\JennyBodyRenderer.java" del "%BASE%\JennyBodyRenderer.java"
if exist "%BASE%\JennyHandRenderer.java" del "%BASE%\JennyHandRenderer.java"
if exist "%BASE%\JennyNpcRenderer.java" del "%BASE%\JennyNpcRenderer.java"
if exist "%BASE%\KoboldColoredRenderer.java" del "%BASE%\KoboldColoredRenderer.java"
if exist "%BASE%\KoboldEntityRenderer.java" del "%BASE%\KoboldEntityRenderer.java"
if exist "%BASE%\KoboldHandRenderer.java" del "%BASE%\KoboldHandRenderer.java"
if exist "%BASE%\KoboldNpcHandRenderer.java" del "%BASE%\KoboldNpcHandRenderer.java"
if exist "%BASE%\KoboldShoulderRenderHandler.java" del "%BASE%\KoboldShoulderRenderHandler.java"
if exist "%BASE%\NpcArmRenderer.java" del "%BASE%\NpcArmRenderer.java"
if exist "%BASE%\NpcBodyRendererAlt.java" del "%BASE%\NpcBodyRendererAlt.java"
if exist "%BASE%\NpcColoredRenderer.java" del "%BASE%\NpcColoredRenderer.java"
if exist "%BASE%\NsfwBoneHidingRenderer.java" del "%BASE%\NsfwBoneHidingRenderer.java"
if exist "%BASE%\PhysicsParticleSystem.java" del "%BASE%\PhysicsParticleSystem.java"
if exist "%BASE%\PyroRenderer.java" del "%BASE%\PyroRenderer.java"
if exist "%BASE%\SimpleNpcHandRenderer.java" del "%BASE%\SimpleNpcHandRenderer.java"
if exist "%BASE%\SlimeHandRenderer.java" del "%BASE%\SlimeHandRenderer.java"
if exist "%BASE%\StaffHandRenderer.java" del "%BASE%\StaffHandRenderer.java"
if exist "%BASE%\TailPhysicsNpcRenderer.java" del "%BASE%\TailPhysicsNpcRenderer.java"

REM -- Move BaseNpcEntity and PlayerKoboldEntity from entity/ to root --
if exist "%BASE%\entity\BaseNpcEntity.java" move "%BASE%\entity\BaseNpcEntity.java" "%BASE%\BaseNpcEntity.java"
if exist "%BASE%\entity\PlayerKoboldEntity.java" move "%BASE%\entity\PlayerKoboldEntity.java" "%BASE%\PlayerKoboldEntity.java"

REM -- Move AnimState from entity/ to registry/ --
if exist "%BASE%\entity\AnimState.java" move "%BASE%\entity\AnimState.java" "%BASE%\registry\AnimState.java"

REM -- Move misplaced root files to correct subfolders --
if exist "%BASE%\GalathSpawnListData.java" move "%BASE%\GalathSpawnListData.java" "%BASE%\data\GalathSpawnListData.java"
if exist "%BASE%\CustomGirlNamesSavedData.java" move "%BASE%\CustomGirlNamesSavedData.java" "%BASE%\data\CustomGirlNamesSavedData.java"
if exist "%BASE%\SetModelCodeCommand.java" move "%BASE%\SetModelCodeCommand.java" "%BASE%\command\SetModelCodeCommand.java"
if exist "%BASE%\WhitelistServerCommand.java" move "%BASE%\WhitelistServerCommand.java" "%BASE%\command\WhitelistServerCommand.java"
if exist "%BASE%\TribePhase.java" move "%BASE%\TribePhase.java" "%BASE%\tribe\TribePhase.java"

echo Done! Run Gradle build now.
pause
@echo off
set PROJ=%~dp0
set SRC=%PROJ%sexmod_src
set BASE=%PROJ%src\main\java\com\trolmastercard\sexmod

REM -- Verificar que sexmod_src existe y tiene archivos --
if not exist "%SRC%" (
    echo ERROR: No se encontro la carpeta sexmod_src
    echo Asegurate de extraer sexmod_sources.zip como sexmod_src junto a este bat
    pause
    exit /b 1
)

echo Fuente: %SRC%
echo Destino: %BASE%
echo.

REM -- Crear carpetas --
mkdir "%BASE%\client" 2>nul
mkdir "%BASE%\client\anim" 2>nul
mkdir "%BASE%\client\event" 2>nul
mkdir "%BASE%\client\gui" 2>nul
mkdir "%BASE%\client\layer" 2>nul
mkdir "%BASE%\client\model" 2>nul
mkdir "%BASE%\client\particle" 2>nul
mkdir "%BASE%\client\render" 2>nul
mkdir "%BASE%\client\render\layer" 2>nul
mkdir "%BASE%\client\renderer" 2>nul
mkdir "%BASE%\client\screen" 2>nul
mkdir "%BASE%\client\util" 2>nul
mkdir "%BASE%\command" 2>nul
mkdir "%BASE%\data" 2>nul
mkdir "%BASE%\entity" 2>nul
mkdir "%BASE%\entity\ai" 2>nul
mkdir "%BASE%\event" 2>nul
mkdir "%BASE%\handler" 2>nul
mkdir "%BASE%\inventory" 2>nul
mkdir "%BASE%\item" 2>nul
mkdir "%BASE%\network" 2>nul
mkdir "%BASE%\network\packet" 2>nul
mkdir "%BASE%\potion" 2>nul
mkdir "%BASE%\registry" 2>nul
mkdir "%BASE%\tribe" 2>nul
mkdir "%BASE%\util" 2>nul
mkdir "%BASE%\world" 2>nul

REM -- Copiar archivos --
set COUNT=0
copy "%SRC%\AgeWarningScreen.java" "%BASE%\AgeWarningScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AgeWarningScreen.java)
copy "%SRC%\AllieBodyRenderer.java" "%BASE%\client\renderer\AllieBodyRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AllieBodyRenderer.java)
copy "%SRC%\AllieEntity.java" "%BASE%\entity\AllieEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AllieEntity.java)
copy "%SRC%\AllieLampModel.java" "%BASE%\client\model\AllieLampModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AllieLampModel.java)
copy "%SRC%\AllieModel.java" "%BASE%\client\model\AllieModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AllieModel.java)
copy "%SRC%\AlliePlayerKobold.java" "%BASE%\AlliePlayerKobold.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AlliePlayerKobold.java)
copy "%SRC%\AllieRenderer.java" "%BASE%\client\renderer\AllieRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AllieRenderer.java)
copy "%SRC%\AlliesLampItem.java" "%BASE%\item\AlliesLampItem.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AlliesLampItem.java)
copy "%SRC%\AlliesLampItemRenderer.java" "%BASE%\client\renderer\AlliesLampItemRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AlliesLampItemRenderer.java)
copy "%SRC%\AngleTarget.java" "%BASE%\util\AngleTarget.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AngleTarget.java)
copy "%SRC%\AngleUtil.java" "%BASE%\util\AngleUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AngleUtil.java)
copy "%SRC%\AnimState.java" "%BASE%\registry\AnimState.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: AnimState.java)
copy "%SRC%\ArmorDamageHandler.java" "%BASE%\event\ArmorDamageHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ArmorDamageHandler.java)
copy "%SRC%\BaseNpcEntity.java" "%BASE%\BaseNpcEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BaseNpcEntity.java)
copy "%SRC%\BaseNpcModel.java" "%BASE%\client\model\BaseNpcModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BaseNpcModel.java)
copy "%SRC%\BaseNpcRenderer.java" "%BASE%\client\renderer\BaseNpcRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BaseNpcRenderer.java)
copy "%SRC%\BeeBodyRenderer.java" "%BASE%\client\renderer\BeeBodyRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BeeBodyRenderer.java)
copy "%SRC%\BeeEntity.java" "%BASE%\entity\BeeEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BeeEntity.java)
copy "%SRC%\BeeModel.java" "%BASE%\client\model\BeeModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BeeModel.java)
copy "%SRC%\BeeOpenChestPacket.java" "%BASE%\network\packet\BeeOpenChestPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BeeOpenChestPacket.java)
copy "%SRC%\BeePlayerKobold.java" "%BASE%\BeePlayerKobold.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BeePlayerKobold.java)
copy "%SRC%\BeeQuickAccessScreen.java" "%BASE%\client\screen\BeeQuickAccessScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BeeQuickAccessScreen.java)
copy "%SRC%\BiMap.java" "%BASE%\util\BiMap.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BiMap.java)
copy "%SRC%\BiaEntity.java" "%BASE%\entity\BiaEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BiaEntity.java)
copy "%SRC%\BiaModel.java" "%BASE%\client\model\BiaModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BiaModel.java)
copy "%SRC%\BiaPlayerKobold.java" "%BASE%\BiaPlayerKobold.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BiaPlayerKobold.java)
copy "%SRC%\BlockEventHandler.java" "%BASE%\handler\BlockEventHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BlockEventHandler.java)
copy "%SRC%\BlockHighlightRenderer.java" "%BASE%\client\renderer\BlockHighlightRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BlockHighlightRenderer.java)
copy "%SRC%\BoneMatrixUtil.java" "%BASE%\util\BoneMatrixUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: BoneMatrixUtil.java)
copy "%SRC%\CachedAnimationProcessor.java" "%BASE%\client\anim\CachedAnimationProcessor.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CachedAnimationProcessor.java)
copy "%SRC%\CachedGeoModel.java" "%BASE%\client\model\CachedGeoModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CachedGeoModel.java)
copy "%SRC%\CameraControlPacket.java" "%BASE%\network\CameraControlPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CameraControlPacket.java)
copy "%SRC%\CancelTaskPacket.java" "%BASE%\network\packet\CancelTaskPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CancelTaskPacket.java)
copy "%SRC%\CatActivateFishingPacket.java" "%BASE%\network\packet\CatActivateFishingPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CatActivateFishingPacket.java)
copy "%SRC%\CatEatingDonePacket.java" "%BASE%\network\CatEatingDonePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CatEatingDonePacket.java)
copy "%SRC%\CatModel.java" "%BASE%\client\model\CatModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CatModel.java)
copy "%SRC%\CatPlayerKobold.java" "%BASE%\CatPlayerKobold.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CatPlayerKobold.java)
copy "%SRC%\CatThrowAwayItemPacket.java" "%BASE%\network\packet\CatThrowAwayItemPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CatThrowAwayItemPacket.java)
copy "%SRC%\ChangeDataParameterPacket.java" "%BASE%\network\packet\ChangeDataParameterPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ChangeDataParameterPacket.java)
copy "%SRC%\ClaimTribePacket.java" "%BASE%\network\packet\ClaimTribePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ClaimTribePacket.java)
copy "%SRC%\ClientProxy.java" "%BASE%\ClientProxy.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ClientProxy.java)
copy "%SRC%\ClientStateManager.java" "%BASE%\client\ClientStateManager.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ClientStateManager.java)
copy "%SRC%\ClothingOverlayEntity.java" "%BASE%\entity\ClothingOverlayEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ClothingOverlayEntity.java)
copy "%SRC%\ClothingOverlayModel.java" "%BASE%\client\model\ClothingOverlayModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ClothingOverlayModel.java)
copy "%SRC%\ClothingScrollWidget.java" "%BASE%\client\gui\ClothingScrollWidget.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ClothingScrollWidget.java)
copy "%SRC%\ClothingSlot.java" "%BASE%\ClothingSlot.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ClothingSlot.java)
copy "%SRC%\ColoredNpcArmRenderer.java" "%BASE%\client\renderer\ColoredNpcArmRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ColoredNpcArmRenderer.java)
copy "%SRC%\ColoredNpcHandRenderer.java" "%BASE%\client\renderer\ColoredNpcHandRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ColoredNpcHandRenderer.java)
copy "%SRC%\CommonProxy.java" "%BASE%\CommonProxy.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CommonProxy.java)
copy "%SRC%\CummyParticleRenderer.java" "%BASE%\client\renderer\CummyParticleRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CummyParticleRenderer.java)
copy "%SRC%\CustomAnimationController.java" "%BASE%\client\anim\CustomAnimationController.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CustomAnimationController.java)
copy "%SRC%\CustomGirlNamesSavedData.java" "%BASE%\CustomGirlNamesSavedData.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CustomGirlNamesSavedData.java)
copy "%SRC%\CustomModelManager.java" "%BASE%\client\model\CustomModelManager.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CustomModelManager.java)
copy "%SRC%\CustomModelSavedData.java" "%BASE%\data\CustomModelSavedData.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CustomModelSavedData.java)
copy "%SRC%\CustomizeNpcPacket.java" "%BASE%\network\packet\CustomizeNpcPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: CustomizeNpcPacket.java)
copy "%SRC%\DespawnClothingPacket.java" "%BASE%\network\packet\DespawnClothingPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: DespawnClothingPacket.java)
copy "%SRC%\DevToolsHandler.java" "%BASE%\event\DevToolsHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: DevToolsHandler.java)
copy "%SRC%\DirectionKey.java" "%BASE%\util\DirectionKey.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: DirectionKey.java)
copy "%SRC%\EggModel.java" "%BASE%\client\model\EggModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EggModel.java)
copy "%SRC%\ElEntityRenderer.java" "%BASE%\client\renderer\ElEntityRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ElEntityRenderer.java)
copy "%SRC%\EllieEntity.java" "%BASE%\entity\EllieEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EllieEntity.java)
copy "%SRC%\EllieModel.java" "%BASE%\EllieModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EllieModel.java)
copy "%SRC%\EllieNpcRenderer.java" "%BASE%\client\renderer\EllieNpcRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EllieNpcRenderer.java)
copy "%SRC%\ElliePlayerKobold.java" "%BASE%\ElliePlayerKobold.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ElliePlayerKobold.java)
copy "%SRC%\ElytraLayer.java" "%BASE%\client\layer\ElytraLayer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ElytraLayer.java)
copy "%SRC%\EnderPearlModel.java" "%BASE%\EnderPearlModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EnderPearlModel.java)
copy "%SRC%\EnergyBallEntity.java" "%BASE%\entity\EnergyBallEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EnergyBallEntity.java)
copy "%SRC%\EnergyBallLegacyModel.java" "%BASE%\EnergyBallLegacyModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EnergyBallLegacyModel.java)
copy "%SRC%\EnergyBallRenderer.java" "%BASE%\client\render\EnergyBallRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EnergyBallRenderer.java)
copy "%SRC%\EntityRenderRegistry.java" "%BASE%\EntityRenderRegistry.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EntityRenderRegistry.java)
copy "%SRC%\EntityUtil.java" "%BASE%\EntityUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EntityUtil.java)
copy "%SRC%\EscapeMinigame.java" "%BASE%\EscapeMinigame.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EscapeMinigame.java)
copy "%SRC%\EventRegistrar.java" "%BASE%\EventRegistrar.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EventRegistrar.java)
copy "%SRC%\ExNpcRenderer.java" "%BASE%\client\renderer\ExNpcRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ExNpcRenderer.java)
copy "%SRC%\EyeAndKoboldColor.java" "%BASE%\EyeAndKoboldColor.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EyeAndKoboldColor.java)
copy "%SRC%\EyeColor.java" "%BASE%\EyeColor.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: EyeColor.java)
copy "%SRC%\FakeClientNetHandler.java" "%BASE%\network\FakeClientNetHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FakeClientNetHandler.java)
copy "%SRC%\FakeNetworkManager.java" "%BASE%\network\FakeNetworkManager.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FakeNetworkManager.java)
copy "%SRC%\FakeWorld.java" "%BASE%\client\FakeWorld.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FakeWorld.java)
copy "%SRC%\FallTreePacket.java" "%BASE%\network\packet\FallTreePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FallTreePacket.java)
copy "%SRC%\FigureNpcRenderer.java" "%BASE%\client\renderer\FigureNpcRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FigureNpcRenderer.java)
copy "%SRC%\FishingHookEntity.java" "%BASE%\entity\FishingHookEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FishingHookEntity.java)
copy "%SRC%\FishingLineRenderer.java" "%BASE%\client\renderer\FishingLineRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FishingLineRenderer.java)
copy "%SRC%\FishingLineSegmentRenderer.java" "%BASE%\client\renderer\FishingLineSegmentRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FishingLineSegmentRenderer.java)
copy "%SRC%\FishingRodBoneModel.java" "%BASE%\FishingRodBoneModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FishingRodBoneModel.java)
copy "%SRC%\ForcePlayerGirlUpdatePacket.java" "%BASE%\network\ForcePlayerGirlUpdatePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ForcePlayerGirlUpdatePacket.java)
copy "%SRC%\FutaCommand.java" "%BASE%\command\FutaCommand.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FutaCommand.java)
copy "%SRC%\FzEntityRenderer.java" "%BASE%\client\renderer\FzEntityRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: FzEntityRenderer.java)
copy "%SRC%\GalathActionCallback.java" "%BASE%\GalathActionCallback.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathActionCallback.java)
copy "%SRC%\GalathAttackPredicate.java" "%BASE%\GalathAttackPredicate.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathAttackPredicate.java)
copy "%SRC%\GalathAttackState.java" "%BASE%\entity\GalathAttackState.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathAttackState.java)
copy "%SRC%\GalathBackOffPacket.java" "%BASE%\network\packet\GalathBackOffPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathBackOffPacket.java)
copy "%SRC%\GalathCallback.java" "%BASE%\GalathCallback.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathCallback.java)
copy "%SRC%\GalathCoinItem.java" "%BASE%\item\GalathCoinItem.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathCoinItem.java)
copy "%SRC%\GalathCoinModel.java" "%BASE%\client\model\GalathCoinModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathCoinModel.java)
copy "%SRC%\GalathCoinRenderer.java" "%BASE%\client\renderer\GalathCoinRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathCoinRenderer.java)
copy "%SRC%\GalathCombatDamageSource.java" "%BASE%\entity\GalathCombatDamageSource.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathCombatDamageSource.java)
copy "%SRC%\GalathDamageSource.java" "%BASE%\entity\GalathDamageSource.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathDamageSource.java)
copy "%SRC%\GalathEntity.java" "%BASE%\entity\GalathEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathEntity.java)
copy "%SRC%\GalathEntityRegistry.java" "%BASE%\GalathEntityRegistry.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathEntityRegistry.java)
copy "%SRC%\GalathFlightController.java" "%BASE%\entity\GalathFlightController.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathFlightController.java)
copy "%SRC%\GalathModel.java" "%BASE%\client\model\GalathModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathModel.java)
copy "%SRC%\GalathOwnershipData.java" "%BASE%\data\GalathOwnershipData.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathOwnershipData.java)
copy "%SRC%\GalathPlayerKobold.java" "%BASE%\GalathPlayerKobold.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathPlayerKobold.java)
copy "%SRC%\GalathPredicate.java" "%BASE%\GalathPredicate.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathPredicate.java)
copy "%SRC%\GalathRapePouncePacket.java" "%BASE%\network\packet\GalathRapePouncePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathRapePouncePacket.java)
copy "%SRC%\GalathRenderer.java" "%BASE%\client\renderer\GalathRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathRenderer.java)
copy "%SRC%\GalathSexCallback.java" "%BASE%\GalathSexCallback.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathSexCallback.java)
copy "%SRC%\GalathSpawnListData.java" "%BASE%\GalathSpawnListData.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GalathSpawnListData.java)
copy "%SRC%\GirlSpecificEntity.java" "%BASE%\entity\GirlSpecificEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GirlSpecificEntity.java)
copy "%SRC%\GoblinBodyRenderer.java" "%BASE%\client\renderer\GoblinBodyRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GoblinBodyRenderer.java)
copy "%SRC%\GoblinColor.java" "%BASE%\GoblinColor.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GoblinColor.java)
copy "%SRC%\GoblinContextMenuScreen.java" "%BASE%\GoblinContextMenuScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GoblinContextMenuScreen.java)
copy "%SRC%\GoblinEntity.java" "%BASE%\GoblinEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GoblinEntity.java)
copy "%SRC%\GoblinEntityRenderer.java" "%BASE%\client\renderer\GoblinEntityRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GoblinEntityRenderer.java)
copy "%SRC%\GoblinHandRenderer.java" "%BASE%\client\renderer\GoblinHandRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GoblinHandRenderer.java)
copy "%SRC%\GoblinModel.java" "%BASE%\client\model\GoblinModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GoblinModel.java)
copy "%SRC%\GoblinMovementState.java" "%BASE%\GoblinMovementState.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GoblinMovementState.java)
copy "%SRC%\GoblinPlayerKobold.java" "%BASE%\GoblinPlayerKobold.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GoblinPlayerKobold.java)
copy "%SRC%\GuiHandler.java" "%BASE%\GuiHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: GuiHandler.java)
copy "%SRC%\HornyMeterOverlay.java" "%BASE%\client\HornyMeterOverlay.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: HornyMeterOverlay.java)
copy "%SRC%\HornyPotion.java" "%BASE%\potion\HornyPotion.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: HornyPotion.java)
copy "%SRC%\IBoneAccessor.java" "%BASE%\client\model\IBoneAccessor.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: IBoneAccessor.java)
copy "%SRC%\IBoneFilter.java" "%BASE%\client\model\IBoneFilter.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: IBoneFilter.java)
copy "%SRC%\IShouldFollowLook.java" "%BASE%\IShouldFollowLook.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: IShouldFollowLook.java)
copy "%SRC%\JennyBodyRenderer.java" "%BASE%\client\renderer\JennyBodyRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: JennyBodyRenderer.java)
copy "%SRC%\JennyEntity.java" "%BASE%\entity\JennyEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: JennyEntity.java)
copy "%SRC%\JennyHandRenderer.java" "%BASE%\client\renderer\JennyHandRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: JennyHandRenderer.java)
copy "%SRC%\JennyModel.java" "%BASE%\client\model\JennyModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: JennyModel.java)
copy "%SRC%\JennyNpcRenderer.java" "%BASE%\client\renderer\JennyNpcRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: JennyNpcRenderer.java)
copy "%SRC%\JennyPlayerKobold.java" "%BASE%\JennyPlayerKobold.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: JennyPlayerKobold.java)
copy "%SRC%\KoboldChestContainer.java" "%BASE%\inventory\KoboldChestContainer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldChestContainer.java)
copy "%SRC%\KoboldColorVariant.java" "%BASE%\entity\KoboldColorVariant.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldColorVariant.java)
copy "%SRC%\KoboldColoredRenderer.java" "%BASE%\client\renderer\KoboldColoredRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldColoredRenderer.java)
copy "%SRC%\KoboldEgg.java" "%BASE%\entity\KoboldEgg.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEgg.java)
copy "%SRC%\KoboldEggEntity.java" "%BASE%\KoboldEggEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEggEntity.java)
copy "%SRC%\KoboldEggEntityRenderer.java" "%BASE%\client\renderer\KoboldEggEntityRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEggEntityRenderer.java)
copy "%SRC%\KoboldEggGeoModel.java" "%BASE%\KoboldEggGeoModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEggGeoModel.java)
copy "%SRC%\KoboldEggItemModel.java" "%BASE%\client\model\KoboldEggItemModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEggItemModel.java)
copy "%SRC%\KoboldEggItemRenderer.java" "%BASE%\client\renderer\KoboldEggItemRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEggItemRenderer.java)
copy "%SRC%\KoboldEggOuterLayer.java" "%BASE%\client\render\layer\KoboldEggOuterLayer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEggOuterLayer.java)
copy "%SRC%\KoboldEggRenderer.java" "%BASE%\client\renderer\KoboldEggRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEggRenderer.java)
copy "%SRC%\KoboldEggSpawnItem.java" "%BASE%\item\KoboldEggSpawnItem.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEggSpawnItem.java)
copy "%SRC%\KoboldEntity.java" "%BASE%\entity\KoboldEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEntity.java)
copy "%SRC%\KoboldEntityRenderer.java" "%BASE%\client\renderer\KoboldEntityRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldEntityRenderer.java)
copy "%SRC%\KoboldFollowLeaderGoal.java" "%BASE%\entity\ai\KoboldFollowLeaderGoal.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldFollowLeaderGoal.java)
copy "%SRC%\KoboldHandRenderer.java" "%BASE%\client\renderer\KoboldHandRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldHandRenderer.java)
copy "%SRC%\KoboldModel.java" "%BASE%\client\model\KoboldModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldModel.java)
copy "%SRC%\KoboldName.java" "%BASE%\entity\KoboldName.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldName.java)
copy "%SRC%\KoboldNameList.java" "%BASE%\entity\KoboldNameList.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldNameList.java)
copy "%SRC%\KoboldNpcHandRenderer.java" "%BASE%\client\renderer\KoboldNpcHandRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldNpcHandRenderer.java)
copy "%SRC%\KoboldRenderer.java" "%BASE%\client\renderer\KoboldRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldRenderer.java)
copy "%SRC%\KoboldShoulderRenderHandler.java" "%BASE%\client\renderer\KoboldShoulderRenderHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldShoulderRenderHandler.java)
copy "%SRC%\KoboldStaffModel.java" "%BASE%\client\model\KoboldStaffModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: KoboldStaffModel.java)
copy "%SRC%\LightUtil.java" "%BASE%\util\LightUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: LightUtil.java)
copy "%SRC%\LightingMode.java" "%BASE%\entity\LightingMode.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: LightingMode.java)
copy "%SRC%\LocateGoblinLairCommand.java" "%BASE%\command\LocateGoblinLairCommand.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: LocateGoblinLairCommand.java)
copy "%SRC%\LunaEntity.java" "%BASE%\entity\LunaEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: LunaEntity.java)
copy "%SRC%\LunaRodItem.java" "%BASE%\item\LunaRodItem.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: LunaRodItem.java)
copy "%SRC%\Main.java" "%BASE%\Main.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: Main.java)
copy "%SRC%\MakeRichWishPacket.java" "%BASE%\network\packet\MakeRichWishPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: MakeRichWishPacket.java)
copy "%SRC%\MangleLieAvoidGoal.java" "%BASE%\entity\ai\MangleLieAvoidGoal.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: MangleLieAvoidGoal.java)
copy "%SRC%\MangleLieEntity.java" "%BASE%\entity\MangleLieEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: MangleLieEntity.java)
copy "%SRC%\MangleLieModel.java" "%BASE%\client\model\MangleLieModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: MangleLieModel.java)
copy "%SRC%\MangleLieRenderer.java" "%BASE%\client\renderer\MangleLieRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: MangleLieRenderer.java)
copy "%SRC%\MangleLieSexRenderer.java" "%BASE%\client\renderer\MangleLieSexRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: MangleLieSexRenderer.java)
copy "%SRC%\MathUtil.java" "%BASE%\util\MathUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: MathUtil.java)
copy "%SRC%\MenuClearHandler.java" "%BASE%\client\event\MenuClearHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: MenuClearHandler.java)
copy "%SRC%\MineAreaPacket.java" "%BASE%\network\packet\MineAreaPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: MineAreaPacket.java)
copy "%SRC%\MineBlocksPacket.java" "%BASE%\network\packet\MineBlocksPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: MineBlocksPacket.java)
copy "%SRC%\ModConstants.java" "%BASE%\ModConstants.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ModConstants.java)
copy "%SRC%\ModEntityRegistry.java" "%BASE%\ModEntityRegistry.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ModEntityRegistry.java)
copy "%SRC%\ModItems.java" "%BASE%\registry\ModItems.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ModItems.java)
copy "%SRC%\ModLootTables.java" "%BASE%\registry\ModLootTables.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ModLootTables.java)
copy "%SRC%\ModNetwork.java" "%BASE%\network\ModNetwork.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ModNetwork.java)
copy "%SRC%\ModSounds.java" "%BASE%\registry\ModSounds.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ModSounds.java)
copy "%SRC%\ModUtil.java" "%BASE%\util\ModUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ModUtil.java)
copy "%SRC%\ModelListPacket.java" "%BASE%\network\packet\ModelListPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ModelListPacket.java)
copy "%SRC%\NameTagEventHandler.java" "%BASE%\handler\NameTagEventHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NameTagEventHandler.java)
copy "%SRC%\NameTribeScreen.java" "%BASE%\NameTribeScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NameTribeScreen.java)
copy "%SRC%\NpcActionCallback.java" "%BASE%\NpcActionCallback.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcActionCallback.java)
copy "%SRC%\NpcActionScreen.java" "%BASE%\NpcActionScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcActionScreen.java)
copy "%SRC%\NpcArmRenderer.java" "%BASE%\client\renderer\NpcArmRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcArmRenderer.java)
copy "%SRC%\NpcBodyRendererAlt.java" "%BASE%\client\renderer\NpcBodyRendererAlt.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcBodyRendererAlt.java)
copy "%SRC%\NpcBoneQuadBuilder.java" "%BASE%\client\render\NpcBoneQuadBuilder.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcBoneQuadBuilder.java)
copy "%SRC%\NpcBreedGoal.java" "%BASE%\entity\ai\NpcBreedGoal.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcBreedGoal.java)
copy "%SRC%\NpcColoredRenderer.java" "%BASE%\client\renderer\NpcColoredRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcColoredRenderer.java)
copy "%SRC%\NpcCombatGoal.java" "%BASE%\entity\ai\NpcCombatGoal.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcCombatGoal.java)
copy "%SRC%\NpcCustomizeScreen.java" "%BASE%\client\gui\NpcCustomizeScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcCustomizeScreen.java)
copy "%SRC%\NpcDamageHandler.java" "%BASE%\event\NpcDamageHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcDamageHandler.java)
copy "%SRC%\NpcDeathHandler.java" "%BASE%\event\NpcDeathHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcDeathHandler.java)
copy "%SRC%\NpcEditorWandItem.java" "%BASE%\item\NpcEditorWandItem.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcEditorWandItem.java)
copy "%SRC%\NpcEnderPearl.java" "%BASE%\NpcEnderPearl.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcEnderPearl.java)
copy "%SRC%\NpcEquipmentContainer.java" "%BASE%\NpcEquipmentContainer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcEquipmentContainer.java)
copy "%SRC%\NpcEquipmentScreen.java" "%BASE%\client\gui\NpcEquipmentScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcEquipmentScreen.java)
copy "%SRC%\NpcEquipmentSlot.java" "%BASE%\NpcEquipmentSlot.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcEquipmentSlot.java)
copy "%SRC%\NpcFloatSupplier.java" "%BASE%\NpcFloatSupplier.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcFloatSupplier.java)
copy "%SRC%\NpcGirlInterface.java" "%BASE%\NpcGirlInterface.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcGirlInterface.java)
copy "%SRC%\NpcGoalBase.java" "%BASE%\entity\ai\NpcGoalBase.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcGoalBase.java)
copy "%SRC%\NpcHandRenderer.java" "%BASE%\client\renderer\NpcHandRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcHandRenderer.java)
copy "%SRC%\NpcInteractScreen.java" "%BASE%\NpcInteractScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcInteractScreen.java)
copy "%SRC%\NpcInventoryBase.java" "%BASE%\NpcInventoryBase.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcInventoryBase.java)
copy "%SRC%\NpcInventoryContainer.java" "%BASE%\inventory\NpcInventoryContainer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcInventoryContainer.java)
copy "%SRC%\NpcInventoryEntity.java" "%BASE%\NpcInventoryEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcInventoryEntity.java)
copy "%SRC%\NpcInventoryGuiScreen.java" "%BASE%\NpcInventoryGuiScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcInventoryGuiScreen.java)
copy "%SRC%\NpcInventoryRenderer.java" "%BASE%\NpcInventoryRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcInventoryRenderer.java)
copy "%SRC%\NpcInventoryScreen.java" "%BASE%\client\gui\NpcInventoryScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcInventoryScreen.java)
copy "%SRC%\NpcLootTables.java" "%BASE%\NpcLootTables.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcLootTables.java)
copy "%SRC%\NpcModelCodeEntity.java" "%BASE%\NpcModelCodeEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcModelCodeEntity.java)
copy "%SRC%\NpcOpenDoorGoal.java" "%BASE%\entity\ai\NpcOpenDoorGoal.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcOpenDoorGoal.java)
copy "%SRC%\NpcQueryInterface.java" "%BASE%\entity\NpcQueryInterface.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcQueryInterface.java)
copy "%SRC%\NpcRenderEventHandler.java" "%BASE%\client\event\NpcRenderEventHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcRenderEventHandler.java)
copy "%SRC%\NpcRenderUtil.java" "%BASE%\client\util\NpcRenderUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcRenderUtil.java)
copy "%SRC%\NpcStateAccessor.java" "%BASE%\entity\NpcStateAccessor.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcStateAccessor.java)
copy "%SRC%\NpcSubtypeRenderer.java" "%BASE%\NpcSubtypeRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcSubtypeRenderer.java)
copy "%SRC%\NpcType.java" "%BASE%\NpcType.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcType.java)
copy "%SRC%\NpcTypeSelectScreen.java" "%BASE%\client\screen\NpcTypeSelectScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcTypeSelectScreen.java)
copy "%SRC%\NpcWorldUtil.java" "%BASE%\util\NpcWorldUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NpcWorldUtil.java)
copy "%SRC%\NsfwBoneHidingRenderer.java" "%BASE%\client\renderer\NsfwBoneHidingRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: NsfwBoneHidingRenderer.java)
copy "%SRC%\OpenModelSelectPacket.java" "%BASE%\network\packet\OpenModelSelectPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: OpenModelSelectPacket.java)
copy "%SRC%\OpenNpcInventoryPacket.java" "%BASE%\network\packet\OpenNpcInventoryPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: OpenNpcInventoryPacket.java)
copy "%SRC%\OutlineShaderManager.java" "%BASE%\client\OutlineShaderManager.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: OutlineShaderManager.java)
copy "%SRC%\OwnershipSyncPacket.java" "%BASE%\network\OwnershipSyncPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: OwnershipSyncPacket.java)
copy "%SRC%\PathUtil.java" "%BASE%\util\PathUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PathUtil.java)
copy "%SRC%\PhysicsBoneUtil.java" "%BASE%\util\PhysicsBoneUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PhysicsBoneUtil.java)
copy "%SRC%\PhysicsParticle.java" "%BASE%\util\PhysicsParticle.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PhysicsParticle.java)
copy "%SRC%\PhysicsParticleSystem.java" "%BASE%\util\PhysicsParticleSystem.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PhysicsParticleSystem.java)
copy "%SRC%\PlayerCamEventHandler.java" "%BASE%\client\PlayerCamEventHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PlayerCamEventHandler.java)
copy "%SRC%\PlayerConnectionHandler.java" "%BASE%\event\PlayerConnectionHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PlayerConnectionHandler.java)
copy "%SRC%\PlayerKoboldEntity.java" "%BASE%\PlayerKoboldEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PlayerKoboldEntity.java)
copy "%SRC%\PlayerKoboldRenderHandler.java" "%BASE%\client\PlayerKoboldRenderHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PlayerKoboldRenderHandler.java)
copy "%SRC%\PlayerKoboldRenderer.java" "%BASE%\client\renderer\PlayerKoboldRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PlayerKoboldRenderer.java)
copy "%SRC%\PlayerSexEventHandler.java" "%BASE%\event\PlayerSexEventHandler.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PlayerSexEventHandler.java)
copy "%SRC%\PlayerSkinUtil.java" "%BASE%\util\PlayerSkinUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PlayerSkinUtil.java)
copy "%SRC%\PropModel.java" "%BASE%\client\model\PropModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PropModel.java)
copy "%SRC%\PyroRenderer.java" "%BASE%\client\renderer\PyroRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: PyroRenderer.java)
copy "%SRC%\Rect2D.java" "%BASE%\util\Rect2D.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: Rect2D.java)
copy "%SRC%\ReloadCustomModelsCommand.java" "%BASE%\command\ReloadCustomModelsCommand.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ReloadCustomModelsCommand.java)
copy "%SRC%\RemoveItemsPacket.java" "%BASE%\network\packet\RemoveItemsPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: RemoveItemsPacket.java)
copy "%SRC%\RequestRidingPacket.java" "%BASE%\network\packet\RequestRidingPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: RequestRidingPacket.java)
copy "%SRC%\RequestServerModelAvailabilityPacket.java" "%BASE%\network\packet\RequestServerModelAvailabilityPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: RequestServerModelAvailabilityPacket.java)
copy "%SRC%\ResetControllerPacket.java" "%BASE%\network\packet\ResetControllerPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ResetControllerPacket.java)
copy "%SRC%\ResetNpcPacket.java" "%BASE%\network\packet\ResetNpcPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ResetNpcPacket.java)
copy "%SRC%\Resettable.java" "%BASE%\entity\Resettable.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: Resettable.java)
copy "%SRC%\RgbColor.java" "%BASE%\util\RgbColor.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: RgbColor.java)
copy "%SRC%\RgbaColor.java" "%BASE%\util\RgbaColor.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: RgbaColor.java)
copy "%SRC%\RgbaColorInner.java" "%BASE%\util\RgbaColorInner.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: RgbaColorInner.java)
copy "%SRC%\SelectableEntityPart.java" "%BASE%\entity\SelectableEntityPart.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SelectableEntityPart.java)
copy "%SRC%\SendChatMessagePacket.java" "%BASE%\network\SendChatMessagePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SendChatMessagePacket.java)
copy "%SRC%\SendCompanionHomePacket.java" "%BASE%\network\SendCompanionHomePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SendCompanionHomePacket.java)
copy "%SRC%\SendEggPacket.java" "%BASE%\network\packet\SendEggPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SendEggPacket.java)
copy "%SRC%\SetModelCodeCommand.java" "%BASE%\SetModelCodeCommand.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SetModelCodeCommand.java)
copy "%SRC%\SetNpcHomePacket.java" "%BASE%\network\packet\SetNpcHomePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SetNpcHomePacket.java)
copy "%SRC%\SetPlayerCamPacket.java" "%BASE%\network\packet\SetPlayerCamPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SetPlayerCamPacket.java)
copy "%SRC%\SetPlayerForNpcPacket.java" "%BASE%\network\packet\SetPlayerForNpcPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SetPlayerForNpcPacket.java)
copy "%SRC%\SetTribeFollowModePacket.java" "%BASE%\network\packet\SetTribeFollowModePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SetTribeFollowModePacket.java)
copy "%SRC%\SexPromptPacket.java" "%BASE%\network\packet\SexPromptPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SexPromptPacket.java)
copy "%SRC%\SexProposalManager.java" "%BASE%\client\SexProposalManager.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SexProposalManager.java)
copy "%SRC%\SexmodDragonBreathParticle.java" "%BASE%\client\particle\SexmodDragonBreathParticle.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SexmodDragonBreathParticle.java)
copy "%SRC%\SexmodFireBlock.java" "%BASE%\world\SexmodFireBlock.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SexmodFireBlock.java)
copy "%SRC%\SexmodStructure.java" "%BASE%\world\SexmodStructure.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SexmodStructure.java)
copy "%SRC%\SexmodStructureConstants.java" "%BASE%\world\SexmodStructureConstants.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SexmodStructureConstants.java)
copy "%SRC%\ShouldFollowLookInterface.java" "%BASE%\ShouldFollowLookInterface.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ShouldFollowLookInterface.java)
copy "%SRC%\SimpleNpcHandRenderer.java" "%BASE%\client\renderer\SimpleNpcHandRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SimpleNpcHandRenderer.java)
copy "%SRC%\SlimeEntity.java" "%BASE%\entity\SlimeEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SlimeEntity.java)
copy "%SRC%\SlimeHandRenderer.java" "%BASE%\client\renderer\SlimeHandRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SlimeHandRenderer.java)
copy "%SRC%\SlimeModel.java" "%BASE%\client\model\SlimeModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SlimeModel.java)
copy "%SRC%\SlimePlayerKobold.java" "%BASE%\SlimePlayerKobold.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SlimePlayerKobold.java)
copy "%SRC%\SpawnEnergyBallParticlesPacket.java" "%BASE%\network\packet\SpawnEnergyBallParticlesPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SpawnEnergyBallParticlesPacket.java)
copy "%SRC%\SpawnParticlePacket.java" "%BASE%\network\packet\SpawnParticlePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SpawnParticlePacket.java)
copy "%SRC%\SpearGripBoneModel.java" "%BASE%\client\model\SpearGripBoneModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SpearGripBoneModel.java)
copy "%SRC%\SpearModel.java" "%BASE%\client\model\SpearModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SpearModel.java)
copy "%SRC%\SpearModelAlt.java" "%BASE%\client\model\SpearModelAlt.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SpearModelAlt.java)
copy "%SRC%\SpearTipBoneModel.java" "%BASE%\client\model\SpearTipBoneModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SpearTipBoneModel.java)
copy "%SRC%\StaffCommandScreen.java" "%BASE%\StaffCommandScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: StaffCommandScreen.java)
copy "%SRC%\StaffHandRenderer.java" "%BASE%\client\renderer\StaffHandRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: StaffHandRenderer.java)
copy "%SRC%\StaffHeadBoneModel.java" "%BASE%\client\model\StaffHeadBoneModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: StaffHeadBoneModel.java)
copy "%SRC%\StaffItem.java" "%BASE%\item\StaffItem.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: StaffItem.java)
copy "%SRC%\StaffItemRenderer.java" "%BASE%\client\renderer\StaffItemRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: StaffItemRenderer.java)
copy "%SRC%\StaffModel.java" "%BASE%\client\model\StaffModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: StaffModel.java)
copy "%SRC%\StartGalathSexPacket.java" "%BASE%\network\packet\StartGalathSexPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: StartGalathSexPacket.java)
copy "%SRC%\StartSexAnimationPacket.java" "%BASE%\network\packet\StartSexAnimationPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: StartSexAnimationPacket.java)
copy "%SRC%\StartupInitFrame.java" "%BASE%\client\StartupInitFrame.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: StartupInitFrame.java)
copy "%SRC%\StructurePlacer.java" "%BASE%\world\StructurePlacer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: StructurePlacer.java)
copy "%SRC%\SummonAlliePacket.java" "%BASE%\network\packet\SummonAlliePacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SummonAlliePacket.java)
copy "%SRC%\SyncCustomModelsPacket.java" "%BASE%\network\packet\SyncCustomModelsPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SyncCustomModelsPacket.java)
copy "%SRC%\SyncInventoryPacket.java" "%BASE%\network\packet\SyncInventoryPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: SyncInventoryPacket.java)
copy "%SRC%\TailPhysicsNpcRenderer.java" "%BASE%\client\renderer\TailPhysicsNpcRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TailPhysicsNpcRenderer.java)
copy "%SRC%\TeleportPlayerPacket.java" "%BASE%\network\packet\TeleportPlayerPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TeleportPlayerPacket.java)
copy "%SRC%\ThreadUtil.java" "%BASE%\util\ThreadUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ThreadUtil.java)
copy "%SRC%\TickableCallback.java" "%BASE%\entity\TickableCallback.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TickableCallback.java)
copy "%SRC%\ToggleableWatchClosestGoal.java" "%BASE%\entity\ai\ToggleableWatchClosestGoal.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ToggleableWatchClosestGoal.java)
copy "%SRC%\ToggleableWatchGoal.java" "%BASE%\entity\ai\ToggleableWatchGoal.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: ToggleableWatchGoal.java)
copy "%SRC%\TransferOwnershipPacket.java" "%BASE%\network\packet\TransferOwnershipPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TransferOwnershipPacket.java)
copy "%SRC%\TransitionScreen.java" "%BASE%\TransitionScreen.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TransitionScreen.java)
copy "%SRC%\TribeAttackGoal.java" "%BASE%\entity\ai\TribeAttackGoal.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TribeAttackGoal.java)
copy "%SRC%\TribeEggItem.java" "%BASE%\item\TribeEggItem.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TribeEggItem.java)
copy "%SRC%\TribeManager.java" "%BASE%\tribe\TribeManager.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TribeManager.java)
copy "%SRC%\TribePhase.java" "%BASE%\TribePhase.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TribePhase.java)
copy "%SRC%\TribeTask.java" "%BASE%\tribe\TribeTask.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TribeTask.java)
copy "%SRC%\TribeUIValuesPacket.java" "%BASE%\network\packet\TribeUIValuesPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TribeUIValuesPacket.java)
copy "%SRC%\TubeRenderer.java" "%BASE%\TubeRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: TubeRenderer.java)
copy "%SRC%\UpdateEquipmentPacket.java" "%BASE%\network\packet\UpdateEquipmentPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: UpdateEquipmentPacket.java)
copy "%SRC%\UpdateGalathVelocityPacket.java" "%BASE%\network\packet\UpdateGalathVelocityPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: UpdateGalathVelocityPacket.java)
copy "%SRC%\UpdatePlayerModelPacket.java" "%BASE%\network\packet\UpdatePlayerModelPacket.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: UpdatePlayerModelPacket.java)
copy "%SRC%\Vec2D.java" "%BASE%\util\Vec2D.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: Vec2D.java)
copy "%SRC%\Vec2i.java" "%BASE%\util\Vec2i.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: Vec2i.java)
copy "%SRC%\VectorMathUtil.java" "%BASE%\util\VectorMathUtil.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: VectorMathUtil.java)
copy "%SRC%\VersionChecker.java" "%BASE%\client\VersionChecker.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: VersionChecker.java)
copy "%SRC%\WanderingEnemyEntity.java" "%BASE%\entity\WanderingEnemyEntity.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: WanderingEnemyEntity.java)
copy "%SRC%\WhitelistServerCommand.java" "%BASE%\WhitelistServerCommand.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: WhitelistServerCommand.java)
copy "%SRC%\WinchesterItem.java" "%BASE%\item\WinchesterItem.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: WinchesterItem.java)
copy "%SRC%\WinchesterModel.java" "%BASE%\client\model\WinchesterModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: WinchesterModel.java)
copy "%SRC%\WinchesterRenderer.java" "%BASE%\WinchesterRenderer.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: WinchesterRenderer.java)
copy "%SRC%\WispFaceModel.java" "%BASE%\client\model\WispFaceModel.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: WispFaceModel.java)
copy "%SRC%\WorldGenerationManager.java" "%BASE%\world\WorldGenerationManager.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: WorldGenerationManager.java)
copy "%SRC%\YawPitch.java" "%BASE%\util\YawPitch.java"
if %errorlevel%==0 (set /a COUNT+=1) else (echo FALLO: YawPitch.java)

echo.
echo Copiados: %COUNT% archivos
pause
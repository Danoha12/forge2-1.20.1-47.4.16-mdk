@echo off
REM ============================================================
REM setup_sexmod.bat
REM Coloca este bat en la raiz del proyecto (donde esta build.gradle)
REM Extrae sexmod_sources.zip en una carpeta llamada 'sexmod_src'
REM en la misma carpeta que este bat, luego ejecutalo.
REM ============================================================

set PROJ=%~dp0
set SRC=%PROJ%sexmod_src
set BASE=%PROJ%src\main\java\com\trolmastercard\sexmod

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
copy "%SRC%\AgeWarningScreen.java" "%BASE%\AgeWarningScreen.java" >nul
copy "%SRC%\AllieBodyRenderer.java" "%BASE%\client\renderer\AllieBodyRenderer.java" >nul
copy "%SRC%\AllieEntity.java" "%BASE%\entity\AllieEntity.java" >nul
copy "%SRC%\AllieLampModel.java" "%BASE%\client\model\AllieLampModel.java" >nul
copy "%SRC%\AllieModel.java" "%BASE%\client\model\AllieModel.java" >nul
copy "%SRC%\AlliePlayerKobold.java" "%BASE%\AlliePlayerKobold.java" >nul
copy "%SRC%\AllieRenderer.java" "%BASE%\client\renderer\AllieRenderer.java" >nul
copy "%SRC%\AlliesLampItem.java" "%BASE%\item\AlliesLampItem.java" >nul
copy "%SRC%\AlliesLampItemRenderer.java" "%BASE%\client\renderer\AlliesLampItemRenderer.java" >nul
copy "%SRC%\AngleTarget.java" "%BASE%\util\AngleTarget.java" >nul
copy "%SRC%\AngleUtil.java" "%BASE%\util\AngleUtil.java" >nul
copy "%SRC%\AnimState.java" "%BASE%\registry\AnimState.java" >nul
copy "%SRC%\ArmorDamageHandler.java" "%BASE%\event\ArmorDamageHandler.java" >nul
copy "%SRC%\BaseNpcEntity.java" "%BASE%\BaseNpcEntity.java" >nul
copy "%SRC%\BaseNpcModel.java" "%BASE%\client\model\BaseNpcModel.java" >nul
copy "%SRC%\BaseNpcRenderer.java" "%BASE%\client\renderer\BaseNpcRenderer.java" >nul
copy "%SRC%\BeeBodyRenderer.java" "%BASE%\client\renderer\BeeBodyRenderer.java" >nul
copy "%SRC%\BeeEntity.java" "%BASE%\entity\BeeEntity.java" >nul
copy "%SRC%\BeeModel.java" "%BASE%\client\model\BeeModel.java" >nul
copy "%SRC%\BeeOpenChestPacket.java" "%BASE%\network\packet\BeeOpenChestPacket.java" >nul
copy "%SRC%\BeePlayerKobold.java" "%BASE%\BeePlayerKobold.java" >nul
copy "%SRC%\BeeQuickAccessScreen.java" "%BASE%\client\screen\BeeQuickAccessScreen.java" >nul
copy "%SRC%\BiMap.java" "%BASE%\util\BiMap.java" >nul
copy "%SRC%\BiaEntity.java" "%BASE%\entity\BiaEntity.java" >nul
copy "%SRC%\BiaModel.java" "%BASE%\client\model\BiaModel.java" >nul
copy "%SRC%\BiaPlayerKobold.java" "%BASE%\BiaPlayerKobold.java" >nul
copy "%SRC%\BlockEventHandler.java" "%BASE%\handler\BlockEventHandler.java" >nul
copy "%SRC%\BlockHighlightRenderer.java" "%BASE%\client\renderer\BlockHighlightRenderer.java" >nul
copy "%SRC%\BoneMatrixUtil.java" "%BASE%\util\BoneMatrixUtil.java" >nul
copy "%SRC%\CachedAnimationProcessor.java" "%BASE%\client\anim\CachedAnimationProcessor.java" >nul
copy "%SRC%\CachedGeoModel.java" "%BASE%\client\model\CachedGeoModel.java" >nul
copy "%SRC%\CameraControlPacket.java" "%BASE%\network\CameraControlPacket.java" >nul
copy "%SRC%\CancelTaskPacket.java" "%BASE%\network\packet\CancelTaskPacket.java" >nul
copy "%SRC%\CatActivateFishingPacket.java" "%BASE%\network\packet\CatActivateFishingPacket.java" >nul
copy "%SRC%\CatEatingDonePacket.java" "%BASE%\network\CatEatingDonePacket.java" >nul
copy "%SRC%\CatModel.java" "%BASE%\client\model\CatModel.java" >nul
copy "%SRC%\CatPlayerKobold.java" "%BASE%\CatPlayerKobold.java" >nul
copy "%SRC%\CatThrowAwayItemPacket.java" "%BASE%\network\packet\CatThrowAwayItemPacket.java" >nul
copy "%SRC%\ChangeDataParameterPacket.java" "%BASE%\network\packet\ChangeDataParameterPacket.java" >nul
copy "%SRC%\ClaimTribePacket.java" "%BASE%\network\packet\ClaimTribePacket.java" >nul
copy "%SRC%\ClientProxy.java" "%BASE%\ClientProxy.java" >nul
copy "%SRC%\ClientStateManager.java" "%BASE%\client\ClientStateManager.java" >nul
copy "%SRC%\ClothingOverlayEntity.java" "%BASE%\entity\ClothingOverlayEntity.java" >nul
copy "%SRC%\ClothingOverlayModel.java" "%BASE%\client\model\ClothingOverlayModel.java" >nul
copy "%SRC%\ClothingScrollWidget.java" "%BASE%\client\gui\ClothingScrollWidget.java" >nul
copy "%SRC%\ClothingSlot.java" "%BASE%\ClothingSlot.java" >nul
copy "%SRC%\ColoredNpcArmRenderer.java" "%BASE%\client\renderer\ColoredNpcArmRenderer.java" >nul
copy "%SRC%\ColoredNpcHandRenderer.java" "%BASE%\client\renderer\ColoredNpcHandRenderer.java" >nul
copy "%SRC%\CommonProxy.java" "%BASE%\CommonProxy.java" >nul
copy "%SRC%\CummyParticleRenderer.java" "%BASE%\client\renderer\CummyParticleRenderer.java" >nul
copy "%SRC%\CustomAnimationController.java" "%BASE%\client\anim\CustomAnimationController.java" >nul
copy "%SRC%\CustomGirlNamesSavedData.java" "%BASE%\CustomGirlNamesSavedData.java" >nul
copy "%SRC%\CustomModelManager.java" "%BASE%\client\model\CustomModelManager.java" >nul
copy "%SRC%\CustomModelSavedData.java" "%BASE%\data\CustomModelSavedData.java" >nul
copy "%SRC%\CustomizeNpcPacket.java" "%BASE%\network\packet\CustomizeNpcPacket.java" >nul
copy "%SRC%\DespawnClothingPacket.java" "%BASE%\network\packet\DespawnClothingPacket.java" >nul
copy "%SRC%\DevToolsHandler.java" "%BASE%\event\DevToolsHandler.java" >nul
copy "%SRC%\DirectionKey.java" "%BASE%\util\DirectionKey.java" >nul
copy "%SRC%\EggModel.java" "%BASE%\client\model\EggModel.java" >nul
copy "%SRC%\ElEntityRenderer.java" "%BASE%\client\renderer\ElEntityRenderer.java" >nul
copy "%SRC%\EllieEntity.java" "%BASE%\entity\EllieEntity.java" >nul
copy "%SRC%\EllieModel.java" "%BASE%\EllieModel.java" >nul
copy "%SRC%\EllieNpcRenderer.java" "%BASE%\client\renderer\EllieNpcRenderer.java" >nul
copy "%SRC%\ElliePlayerKobold.java" "%BASE%\ElliePlayerKobold.java" >nul
copy "%SRC%\ElytraLayer.java" "%BASE%\client\layer\ElytraLayer.java" >nul
copy "%SRC%\EnderPearlModel.java" "%BASE%\EnderPearlModel.java" >nul
copy "%SRC%\EnergyBallEntity.java" "%BASE%\entity\EnergyBallEntity.java" >nul
copy "%SRC%\EnergyBallLegacyModel.java" "%BASE%\EnergyBallLegacyModel.java" >nul
copy "%SRC%\EnergyBallRenderer.java" "%BASE%\client\render\EnergyBallRenderer.java" >nul
copy "%SRC%\EntityRenderRegistry.java" "%BASE%\EntityRenderRegistry.java" >nul
copy "%SRC%\EntityUtil.java" "%BASE%\EntityUtil.java" >nul
copy "%SRC%\EscapeMinigame.java" "%BASE%\EscapeMinigame.java" >nul
copy "%SRC%\EventRegistrar.java" "%BASE%\EventRegistrar.java" >nul
copy "%SRC%\ExNpcRenderer.java" "%BASE%\client\renderer\ExNpcRenderer.java" >nul
copy "%SRC%\EyeAndKoboldColor.java" "%BASE%\EyeAndKoboldColor.java" >nul
copy "%SRC%\EyeColor.java" "%BASE%\EyeColor.java" >nul
copy "%SRC%\FakeClientNetHandler.java" "%BASE%\network\FakeClientNetHandler.java" >nul
copy "%SRC%\FakeNetworkManager.java" "%BASE%\network\FakeNetworkManager.java" >nul
copy "%SRC%\FakeWorld.java" "%BASE%\client\FakeWorld.java" >nul
copy "%SRC%\FallTreePacket.java" "%BASE%\network\packet\FallTreePacket.java" >nul
copy "%SRC%\FigureNpcRenderer.java" "%BASE%\client\renderer\FigureNpcRenderer.java" >nul
copy "%SRC%\FishingHookEntity.java" "%BASE%\entity\FishingHookEntity.java" >nul
copy "%SRC%\FishingLineRenderer.java" "%BASE%\client\renderer\FishingLineRenderer.java" >nul
copy "%SRC%\FishingLineSegmentRenderer.java" "%BASE%\client\renderer\FishingLineSegmentRenderer.java" >nul
copy "%SRC%\FishingRodBoneModel.java" "%BASE%\FishingRodBoneModel.java" >nul
copy "%SRC%\ForcePlayerGirlUpdatePacket.java" "%BASE%\network\ForcePlayerGirlUpdatePacket.java" >nul
copy "%SRC%\FutaCommand.java" "%BASE%\command\FutaCommand.java" >nul
copy "%SRC%\FzEntityRenderer.java" "%BASE%\client\renderer\FzEntityRenderer.java" >nul
copy "%SRC%\GalathActionCallback.java" "%BASE%\GalathActionCallback.java" >nul
copy "%SRC%\GalathAttackPredicate.java" "%BASE%\GalathAttackPredicate.java" >nul
copy "%SRC%\GalathAttackState.java" "%BASE%\entity\GalathAttackState.java" >nul
copy "%SRC%\GalathBackOffPacket.java" "%BASE%\network\packet\GalathBackOffPacket.java" >nul
copy "%SRC%\GalathCallback.java" "%BASE%\GalathCallback.java" >nul
copy "%SRC%\GalathCoinItem.java" "%BASE%\item\GalathCoinItem.java" >nul
copy "%SRC%\GalathCoinModel.java" "%BASE%\client\model\GalathCoinModel.java" >nul
copy "%SRC%\GalathCoinRenderer.java" "%BASE%\client\renderer\GalathCoinRenderer.java" >nul
copy "%SRC%\GalathCombatDamageSource.java" "%BASE%\entity\GalathCombatDamageSource.java" >nul
copy "%SRC%\GalathDamageSource.java" "%BASE%\entity\GalathDamageSource.java" >nul
copy "%SRC%\GalathEntity.java" "%BASE%\entity\GalathEntity.java" >nul
copy "%SRC%\GalathEntityRegistry.java" "%BASE%\GalathEntityRegistry.java" >nul
copy "%SRC%\GalathFlightController.java" "%BASE%\entity\GalathFlightController.java" >nul
copy "%SRC%\GalathModel.java" "%BASE%\client\model\GalathModel.java" >nul
copy "%SRC%\GalathOwnershipData.java" "%BASE%\data\GalathOwnershipData.java" >nul
copy "%SRC%\GalathPlayerKobold.java" "%BASE%\GalathPlayerKobold.java" >nul
copy "%SRC%\GalathPredicate.java" "%BASE%\GalathPredicate.java" >nul
copy "%SRC%\GalathRapePouncePacket.java" "%BASE%\network\packet\GalathRapePouncePacket.java" >nul
copy "%SRC%\GalathRenderer.java" "%BASE%\client\renderer\GalathRenderer.java" >nul
copy "%SRC%\GalathSexCallback.java" "%BASE%\GalathSexCallback.java" >nul
copy "%SRC%\GalathSpawnListData.java" "%BASE%\GalathSpawnListData.java" >nul
copy "%SRC%\GirlSpecificEntity.java" "%BASE%\entity\GirlSpecificEntity.java" >nul
copy "%SRC%\GoblinBodyRenderer.java" "%BASE%\client\renderer\GoblinBodyRenderer.java" >nul
copy "%SRC%\GoblinColor.java" "%BASE%\GoblinColor.java" >nul
copy "%SRC%\GoblinContextMenuScreen.java" "%BASE%\GoblinContextMenuScreen.java" >nul
copy "%SRC%\GoblinEntity.java" "%BASE%\GoblinEntity.java" >nul
copy "%SRC%\GoblinEntityRenderer.java" "%BASE%\client\renderer\GoblinEntityRenderer.java" >nul
copy "%SRC%\GoblinHandRenderer.java" "%BASE%\client\renderer\GoblinHandRenderer.java" >nul
copy "%SRC%\GoblinModel.java" "%BASE%\client\model\GoblinModel.java" >nul
copy "%SRC%\GoblinMovementState.java" "%BASE%\GoblinMovementState.java" >nul
copy "%SRC%\GoblinPlayerKobold.java" "%BASE%\GoblinPlayerKobold.java" >nul
copy "%SRC%\GuiHandler.java" "%BASE%\GuiHandler.java" >nul
copy "%SRC%\HornyMeterOverlay.java" "%BASE%\client\HornyMeterOverlay.java" >nul
copy "%SRC%\HornyPotion.java" "%BASE%\potion\HornyPotion.java" >nul
copy "%SRC%\IBoneAccessor.java" "%BASE%\client\model\IBoneAccessor.java" >nul
copy "%SRC%\IBoneFilter.java" "%BASE%\client\model\IBoneFilter.java" >nul
copy "%SRC%\IShouldFollowLook.java" "%BASE%\IShouldFollowLook.java" >nul
copy "%SRC%\JennyBodyRenderer.java" "%BASE%\client\renderer\JennyBodyRenderer.java" >nul
copy "%SRC%\JennyEntity.java" "%BASE%\entity\JennyEntity.java" >nul
copy "%SRC%\JennyHandRenderer.java" "%BASE%\client\renderer\JennyHandRenderer.java" >nul
copy "%SRC%\JennyModel.java" "%BASE%\client\model\JennyModel.java" >nul
copy "%SRC%\JennyNpcRenderer.java" "%BASE%\client\renderer\JennyNpcRenderer.java" >nul
copy "%SRC%\JennyPlayerKobold.java" "%BASE%\JennyPlayerKobold.java" >nul
copy "%SRC%\KoboldChestContainer.java" "%BASE%\inventory\KoboldChestContainer.java" >nul
copy "%SRC%\KoboldColorVariant.java" "%BASE%\entity\KoboldColorVariant.java" >nul
copy "%SRC%\KoboldColoredRenderer.java" "%BASE%\client\renderer\KoboldColoredRenderer.java" >nul
copy "%SRC%\KoboldEgg.java" "%BASE%\entity\KoboldEgg.java" >nul
copy "%SRC%\KoboldEggEntity.java" "%BASE%\KoboldEggEntity.java" >nul
copy "%SRC%\KoboldEggEntityRenderer.java" "%BASE%\client\renderer\KoboldEggEntityRenderer.java" >nul
copy "%SRC%\KoboldEggGeoModel.java" "%BASE%\KoboldEggGeoModel.java" >nul
copy "%SRC%\KoboldEggItemModel.java" "%BASE%\client\model\KoboldEggItemModel.java" >nul
copy "%SRC%\KoboldEggItemRenderer.java" "%BASE%\client\renderer\KoboldEggItemRenderer.java" >nul
copy "%SRC%\KoboldEggOuterLayer.java" "%BASE%\client\render\layer\KoboldEggOuterLayer.java" >nul
copy "%SRC%\KoboldEggRenderer.java" "%BASE%\client\renderer\KoboldEggRenderer.java" >nul
copy "%SRC%\KoboldEggSpawnItem.java" "%BASE%\item\KoboldEggSpawnItem.java" >nul
copy "%SRC%\KoboldEntity.java" "%BASE%\entity\KoboldEntity.java" >nul
copy "%SRC%\KoboldEntityRenderer.java" "%BASE%\client\renderer\KoboldEntityRenderer.java" >nul
copy "%SRC%\KoboldFollowLeaderGoal.java" "%BASE%\entity\ai\KoboldFollowLeaderGoal.java" >nul
copy "%SRC%\KoboldHandRenderer.java" "%BASE%\client\renderer\KoboldHandRenderer.java" >nul
copy "%SRC%\KoboldModel.java" "%BASE%\client\model\KoboldModel.java" >nul
copy "%SRC%\KoboldName.java" "%BASE%\entity\KoboldName.java" >nul
copy "%SRC%\KoboldNameList.java" "%BASE%\entity\KoboldNameList.java" >nul
copy "%SRC%\KoboldNpcHandRenderer.java" "%BASE%\client\renderer\KoboldNpcHandRenderer.java" >nul
copy "%SRC%\KoboldRenderer.java" "%BASE%\client\renderer\KoboldRenderer.java" >nul
copy "%SRC%\KoboldShoulderRenderHandler.java" "%BASE%\client\renderer\KoboldShoulderRenderHandler.java" >nul
copy "%SRC%\KoboldStaffModel.java" "%BASE%\client\model\KoboldStaffModel.java" >nul
copy "%SRC%\LightUtil.java" "%BASE%\util\LightUtil.java" >nul
copy "%SRC%\LightingMode.java" "%BASE%\entity\LightingMode.java" >nul
copy "%SRC%\LocateGoblinLairCommand.java" "%BASE%\command\LocateGoblinLairCommand.java" >nul
copy "%SRC%\LunaEntity.java" "%BASE%\entity\LunaEntity.java" >nul
copy "%SRC%\LunaRodItem.java" "%BASE%\item\LunaRodItem.java" >nul
copy "%SRC%\Main.java" "%BASE%\Main.java" >nul
copy "%SRC%\MakeRichWishPacket.java" "%BASE%\network\packet\MakeRichWishPacket.java" >nul
copy "%SRC%\MangleLieAvoidGoal.java" "%BASE%\entity\ai\MangleLieAvoidGoal.java" >nul
copy "%SRC%\MangleLieEntity.java" "%BASE%\entity\MangleLieEntity.java" >nul
copy "%SRC%\MangleLieModel.java" "%BASE%\client\model\MangleLieModel.java" >nul
copy "%SRC%\MangleLieRenderer.java" "%BASE%\client\renderer\MangleLieRenderer.java" >nul
copy "%SRC%\MangleLieSexRenderer.java" "%BASE%\client\renderer\MangleLieSexRenderer.java" >nul
copy "%SRC%\MathUtil.java" "%BASE%\util\MathUtil.java" >nul
copy "%SRC%\MenuClearHandler.java" "%BASE%\client\event\MenuClearHandler.java" >nul
copy "%SRC%\MineAreaPacket.java" "%BASE%\network\packet\MineAreaPacket.java" >nul
copy "%SRC%\MineBlocksPacket.java" "%BASE%\network\packet\MineBlocksPacket.java" >nul
copy "%SRC%\ModConstants.java" "%BASE%\ModConstants.java" >nul
copy "%SRC%\ModEntityRegistry.java" "%BASE%\ModEntityRegistry.java" >nul
copy "%SRC%\ModItems.java" "%BASE%\registry\ModItems.java" >nul
copy "%SRC%\ModLootTables.java" "%BASE%\registry\ModLootTables.java" >nul
copy "%SRC%\ModNetwork.java" "%BASE%\network\ModNetwork.java" >nul
copy "%SRC%\ModSounds.java" "%BASE%\registry\ModSounds.java" >nul
copy "%SRC%\ModUtil.java" "%BASE%\util\ModUtil.java" >nul
copy "%SRC%\ModelListPacket.java" "%BASE%\network\packet\ModelListPacket.java" >nul
copy "%SRC%\NameTagEventHandler.java" "%BASE%\handler\NameTagEventHandler.java" >nul
copy "%SRC%\NameTribeScreen.java" "%BASE%\NameTribeScreen.java" >nul
copy "%SRC%\NpcActionCallback.java" "%BASE%\NpcActionCallback.java" >nul
copy "%SRC%\NpcActionScreen.java" "%BASE%\NpcActionScreen.java" >nul
copy "%SRC%\NpcArmRenderer.java" "%BASE%\client\renderer\NpcArmRenderer.java" >nul
copy "%SRC%\NpcBodyRendererAlt.java" "%BASE%\client\renderer\NpcBodyRendererAlt.java" >nul
copy "%SRC%\NpcBoneQuadBuilder.java" "%BASE%\client\render\NpcBoneQuadBuilder.java" >nul
copy "%SRC%\NpcBreedGoal.java" "%BASE%\entity\ai\NpcBreedGoal.java" >nul
copy "%SRC%\NpcColoredRenderer.java" "%BASE%\client\renderer\NpcColoredRenderer.java" >nul
copy "%SRC%\NpcCombatGoal.java" "%BASE%\entity\ai\NpcCombatGoal.java" >nul
copy "%SRC%\NpcCustomizeScreen.java" "%BASE%\client\gui\NpcCustomizeScreen.java" >nul
copy "%SRC%\NpcDamageHandler.java" "%BASE%\event\NpcDamageHandler.java" >nul
copy "%SRC%\NpcDeathHandler.java" "%BASE%\event\NpcDeathHandler.java" >nul
copy "%SRC%\NpcEditorWandItem.java" "%BASE%\item\NpcEditorWandItem.java" >nul
copy "%SRC%\NpcEnderPearl.java" "%BASE%\NpcEnderPearl.java" >nul
copy "%SRC%\NpcEquipmentContainer.java" "%BASE%\NpcEquipmentContainer.java" >nul
copy "%SRC%\NpcEquipmentScreen.java" "%BASE%\client\gui\NpcEquipmentScreen.java" >nul
copy "%SRC%\NpcEquipmentSlot.java" "%BASE%\NpcEquipmentSlot.java" >nul
copy "%SRC%\NpcFloatSupplier.java" "%BASE%\NpcFloatSupplier.java" >nul
copy "%SRC%\NpcGirlInterface.java" "%BASE%\NpcGirlInterface.java" >nul
copy "%SRC%\NpcGoalBase.java" "%BASE%\entity\ai\NpcGoalBase.java" >nul
copy "%SRC%\NpcHandRenderer.java" "%BASE%\client\renderer\NpcHandRenderer.java" >nul
copy "%SRC%\NpcInteractScreen.java" "%BASE%\NpcInteractScreen.java" >nul
copy "%SRC%\NpcInventoryBase.java" "%BASE%\NpcInventoryBase.java" >nul
copy "%SRC%\NpcInventoryContainer.java" "%BASE%\inventory\NpcInventoryContainer.java" >nul
copy "%SRC%\NpcInventoryEntity.java" "%BASE%\NpcInventoryEntity.java" >nul
copy "%SRC%\NpcInventoryGuiScreen.java" "%BASE%\NpcInventoryGuiScreen.java" >nul
copy "%SRC%\NpcInventoryRenderer.java" "%BASE%\NpcInventoryRenderer.java" >nul
copy "%SRC%\NpcInventoryScreen.java" "%BASE%\client\gui\NpcInventoryScreen.java" >nul
copy "%SRC%\NpcLootTables.java" "%BASE%\NpcLootTables.java" >nul
copy "%SRC%\NpcModelCodeEntity.java" "%BASE%\NpcModelCodeEntity.java" >nul
copy "%SRC%\NpcOpenDoorGoal.java" "%BASE%\entity\ai\NpcOpenDoorGoal.java" >nul
copy "%SRC%\NpcQueryInterface.java" "%BASE%\entity\NpcQueryInterface.java" >nul
copy "%SRC%\NpcRenderEventHandler.java" "%BASE%\client\event\NpcRenderEventHandler.java" >nul
copy "%SRC%\NpcRenderUtil.java" "%BASE%\client\util\NpcRenderUtil.java" >nul
copy "%SRC%\NpcStateAccessor.java" "%BASE%\entity\NpcStateAccessor.java" >nul
copy "%SRC%\NpcSubtypeRenderer.java" "%BASE%\NpcSubtypeRenderer.java" >nul
copy "%SRC%\NpcType.java" "%BASE%\NpcType.java" >nul
copy "%SRC%\NpcTypeSelectScreen.java" "%BASE%\client\screen\NpcTypeSelectScreen.java" >nul
copy "%SRC%\NpcWorldUtil.java" "%BASE%\util\NpcWorldUtil.java" >nul
copy "%SRC%\NsfwBoneHidingRenderer.java" "%BASE%\client\renderer\NsfwBoneHidingRenderer.java" >nul
copy "%SRC%\OpenModelSelectPacket.java" "%BASE%\network\packet\OpenModelSelectPacket.java" >nul
copy "%SRC%\OpenNpcInventoryPacket.java" "%BASE%\network\packet\OpenNpcInventoryPacket.java" >nul
copy "%SRC%\OutlineShaderManager.java" "%BASE%\client\OutlineShaderManager.java" >nul
copy "%SRC%\OwnershipSyncPacket.java" "%BASE%\network\OwnershipSyncPacket.java" >nul
copy "%SRC%\PathUtil.java" "%BASE%\util\PathUtil.java" >nul
copy "%SRC%\PhysicsBoneUtil.java" "%BASE%\util\PhysicsBoneUtil.java" >nul
copy "%SRC%\PhysicsParticle.java" "%BASE%\util\PhysicsParticle.java" >nul
copy "%SRC%\PhysicsParticleSystem.java" "%BASE%\util\PhysicsParticleSystem.java" >nul
copy "%SRC%\PlayerCamEventHandler.java" "%BASE%\client\PlayerCamEventHandler.java" >nul
copy "%SRC%\PlayerConnectionHandler.java" "%BASE%\event\PlayerConnectionHandler.java" >nul
copy "%SRC%\PlayerKoboldEntity.java" "%BASE%\PlayerKoboldEntity.java" >nul
copy "%SRC%\PlayerKoboldRenderHandler.java" "%BASE%\client\PlayerKoboldRenderHandler.java" >nul
copy "%SRC%\PlayerKoboldRenderer.java" "%BASE%\client\renderer\PlayerKoboldRenderer.java" >nul
copy "%SRC%\PlayerSexEventHandler.java" "%BASE%\event\PlayerSexEventHandler.java" >nul
copy "%SRC%\PlayerSkinUtil.java" "%BASE%\util\PlayerSkinUtil.java" >nul
copy "%SRC%\PropModel.java" "%BASE%\client\model\PropModel.java" >nul
copy "%SRC%\PyroRenderer.java" "%BASE%\client\renderer\PyroRenderer.java" >nul
copy "%SRC%\Rect2D.java" "%BASE%\util\Rect2D.java" >nul
copy "%SRC%\ReloadCustomModelsCommand.java" "%BASE%\command\ReloadCustomModelsCommand.java" >nul
copy "%SRC%\RemoveItemsPacket.java" "%BASE%\network\packet\RemoveItemsPacket.java" >nul
copy "%SRC%\RequestRidingPacket.java" "%BASE%\network\packet\RequestRidingPacket.java" >nul
copy "%SRC%\RequestServerModelAvailabilityPacket.java" "%BASE%\network\packet\RequestServerModelAvailabilityPacket.java" >nul
copy "%SRC%\ResetControllerPacket.java" "%BASE%\network\packet\ResetControllerPacket.java" >nul
copy "%SRC%\ResetNpcPacket.java" "%BASE%\network\packet\ResetNpcPacket.java" >nul
copy "%SRC%\Resettable.java" "%BASE%\entity\Resettable.java" >nul
copy "%SRC%\RgbColor.java" "%BASE%\util\RgbColor.java" >nul
copy "%SRC%\RgbaColor.java" "%BASE%\util\RgbaColor.java" >nul
copy "%SRC%\RgbaColorInner.java" "%BASE%\util\RgbaColorInner.java" >nul
copy "%SRC%\SelectableEntityPart.java" "%BASE%\entity\SelectableEntityPart.java" >nul
copy "%SRC%\SendChatMessagePacket.java" "%BASE%\network\SendChatMessagePacket.java" >nul
copy "%SRC%\SendCompanionHomePacket.java" "%BASE%\network\SendCompanionHomePacket.java" >nul
copy "%SRC%\SendEggPacket.java" "%BASE%\network\packet\SendEggPacket.java" >nul
copy "%SRC%\SetModelCodeCommand.java" "%BASE%\SetModelCodeCommand.java" >nul
copy "%SRC%\SetNpcHomePacket.java" "%BASE%\network\packet\SetNpcHomePacket.java" >nul
copy "%SRC%\SetPlayerCamPacket.java" "%BASE%\network\packet\SetPlayerCamPacket.java" >nul
copy "%SRC%\SetPlayerForNpcPacket.java" "%BASE%\network\packet\SetPlayerForNpcPacket.java" >nul
copy "%SRC%\SetTribeFollowModePacket.java" "%BASE%\network\packet\SetTribeFollowModePacket.java" >nul
copy "%SRC%\SexPromptPacket.java" "%BASE%\network\packet\SexPromptPacket.java" >nul
copy "%SRC%\SexProposalManager.java" "%BASE%\client\SexProposalManager.java" >nul
copy "%SRC%\SexmodDragonBreathParticle.java" "%BASE%\client\particle\SexmodDragonBreathParticle.java" >nul
copy "%SRC%\SexmodFireBlock.java" "%BASE%\world\SexmodFireBlock.java" >nul
copy "%SRC%\SexmodStructure.java" "%BASE%\world\SexmodStructure.java" >nul
copy "%SRC%\SexmodStructureConstants.java" "%BASE%\world\SexmodStructureConstants.java" >nul
copy "%SRC%\ShouldFollowLookInterface.java" "%BASE%\ShouldFollowLookInterface.java" >nul
copy "%SRC%\SimpleNpcHandRenderer.java" "%BASE%\client\renderer\SimpleNpcHandRenderer.java" >nul
copy "%SRC%\SlimeEntity.java" "%BASE%\entity\SlimeEntity.java" >nul
copy "%SRC%\SlimeHandRenderer.java" "%BASE%\client\renderer\SlimeHandRenderer.java" >nul
copy "%SRC%\SlimeModel.java" "%BASE%\client\model\SlimeModel.java" >nul
copy "%SRC%\SlimePlayerKobold.java" "%BASE%\SlimePlayerKobold.java" >nul
copy "%SRC%\SpawnEnergyBallParticlesPacket.java" "%BASE%\network\packet\SpawnEnergyBallParticlesPacket.java" >nul
copy "%SRC%\SpawnParticlePacket.java" "%BASE%\network\packet\SpawnParticlePacket.java" >nul
copy "%SRC%\SpearGripBoneModel.java" "%BASE%\client\model\SpearGripBoneModel.java" >nul
copy "%SRC%\SpearModel.java" "%BASE%\client\model\SpearModel.java" >nul
copy "%SRC%\SpearModelAlt.java" "%BASE%\client\model\SpearModelAlt.java" >nul
copy "%SRC%\SpearTipBoneModel.java" "%BASE%\client\model\SpearTipBoneModel.java" >nul
copy "%SRC%\StaffCommandScreen.java" "%BASE%\StaffCommandScreen.java" >nul
copy "%SRC%\StaffHandRenderer.java" "%BASE%\client\renderer\StaffHandRenderer.java" >nul
copy "%SRC%\StaffHeadBoneModel.java" "%BASE%\client\model\StaffHeadBoneModel.java" >nul
copy "%SRC%\StaffItem.java" "%BASE%\item\StaffItem.java" >nul
copy "%SRC%\StaffItemRenderer.java" "%BASE%\client\renderer\StaffItemRenderer.java" >nul
copy "%SRC%\StaffModel.java" "%BASE%\client\model\StaffModel.java" >nul
copy "%SRC%\StartGalathSexPacket.java" "%BASE%\network\packet\StartGalathSexPacket.java" >nul
copy "%SRC%\StartSexAnimationPacket.java" "%BASE%\network\packet\StartSexAnimationPacket.java" >nul
copy "%SRC%\StartupInitFrame.java" "%BASE%\client\StartupInitFrame.java" >nul
copy "%SRC%\StructurePlacer.java" "%BASE%\world\StructurePlacer.java" >nul
copy "%SRC%\SummonAlliePacket.java" "%BASE%\network\packet\SummonAlliePacket.java" >nul
copy "%SRC%\SyncCustomModelsPacket.java" "%BASE%\network\packet\SyncCustomModelsPacket.java" >nul
copy "%SRC%\SyncInventoryPacket.java" "%BASE%\network\packet\SyncInventoryPacket.java" >nul
copy "%SRC%\TailPhysicsNpcRenderer.java" "%BASE%\client\renderer\TailPhysicsNpcRenderer.java" >nul
copy "%SRC%\TeleportPlayerPacket.java" "%BASE%\network\packet\TeleportPlayerPacket.java" >nul
copy "%SRC%\ThreadUtil.java" "%BASE%\util\ThreadUtil.java" >nul
copy "%SRC%\TickableCallback.java" "%BASE%\entity\TickableCallback.java" >nul
copy "%SRC%\ToggleableWatchClosestGoal.java" "%BASE%\entity\ai\ToggleableWatchClosestGoal.java" >nul
copy "%SRC%\ToggleableWatchGoal.java" "%BASE%\entity\ai\ToggleableWatchGoal.java" >nul
copy "%SRC%\TransferOwnershipPacket.java" "%BASE%\network\packet\TransferOwnershipPacket.java" >nul
copy "%SRC%\TransitionScreen.java" "%BASE%\TransitionScreen.java" >nul
copy "%SRC%\TribeAttackGoal.java" "%BASE%\entity\ai\TribeAttackGoal.java" >nul
copy "%SRC%\TribeEggItem.java" "%BASE%\item\TribeEggItem.java" >nul
copy "%SRC%\TribeManager.java" "%BASE%\tribe\TribeManager.java" >nul
copy "%SRC%\TribePhase.java" "%BASE%\TribePhase.java" >nul
copy "%SRC%\TribeTask.java" "%BASE%\tribe\TribeTask.java" >nul
copy "%SRC%\TribeUIValuesPacket.java" "%BASE%\network\packet\TribeUIValuesPacket.java" >nul
copy "%SRC%\TubeRenderer.java" "%BASE%\TubeRenderer.java" >nul
copy "%SRC%\UpdateEquipmentPacket.java" "%BASE%\network\packet\UpdateEquipmentPacket.java" >nul
copy "%SRC%\UpdateGalathVelocityPacket.java" "%BASE%\network\packet\UpdateGalathVelocityPacket.java" >nul
copy "%SRC%\UpdatePlayerModelPacket.java" "%BASE%\network\packet\UpdatePlayerModelPacket.java" >nul
copy "%SRC%\Vec2D.java" "%BASE%\util\Vec2D.java" >nul
copy "%SRC%\Vec2i.java" "%BASE%\util\Vec2i.java" >nul
copy "%SRC%\VectorMathUtil.java" "%BASE%\util\VectorMathUtil.java" >nul
copy "%SRC%\VersionChecker.java" "%BASE%\client\VersionChecker.java" >nul
copy "%SRC%\WanderingEnemyEntity.java" "%BASE%\entity\WanderingEnemyEntity.java" >nul
copy "%SRC%\WhitelistServerCommand.java" "%BASE%\WhitelistServerCommand.java" >nul
copy "%SRC%\WinchesterItem.java" "%BASE%\item\WinchesterItem.java" >nul
copy "%SRC%\WinchesterModel.java" "%BASE%\client\model\WinchesterModel.java" >nul
copy "%SRC%\WinchesterRenderer.java" "%BASE%\WinchesterRenderer.java" >nul
copy "%SRC%\WispFaceModel.java" "%BASE%\client\model\WispFaceModel.java" >nul
copy "%SRC%\WorldGenerationManager.java" "%BASE%\world\WorldGenerationManager.java" >nul
copy "%SRC%\YawPitch.java" "%BASE%\util\YawPitch.java" >nul

echo.
echo Listo! Todos los archivos copiados.
pause
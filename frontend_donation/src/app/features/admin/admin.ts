import { Component, computed, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { RolNombre, UserResponse } from '../../core/models/auth.model';
import {
  CampaignResponse,
  CampaignStatus,
  CampaignCategory,
} from '../../core/models/campaign.model';
import { DonationResponse, PagedResponse, PaymentStatus } from '../../core/models/donation.model';
import { AuthService } from '../../core/services/auth';
import { CampaignService } from '../../core/services/campaign.service';
import { DonationService } from '../../core/services/donation.service';
import { ToastService } from '../../core/services/toast.service';
import { UserService } from '../../core/services/user.service';

import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  PASSWORD_REGEX,
  extractApiErrorMessage,
} from '../../core/utils/password-validators';

import { ConfirmModal } from '../../shared/components/confirm-modal/confirm-modal';
import { CustomDatePipe } from '../../shared/pipes/custom-date-pipe';
import { UserAvatarComponent } from '../../shared/components/user-avatar';

export type AdminTab = 'resumen' | 'campanas' | 'donaciones' | 'usuarios';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    CustomDatePipe,
    ConfirmModal,
    UserAvatarComponent,
  ],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class AdminComponent implements OnInit, OnDestroy {
  readonly createUserModalOpen = signal(false);
  readonly savingUser = signal(false);
  readonly createUserError = signal('');
  readonly showAdminUserPw = signal(false);
  readonly editUserModalOpen = signal(false);
  readonly selectedUser = signal<UserResponse | null>(null);
  readonly savingEditUser = signal(false);
  readonly editUserError = signal('');
  readonly userToDelete = signal<UserResponse | null>(null);
  readonly auth = inject(AuthService);
  readonly campaignService = inject(CampaignService);
  private readonly donationService = inject(DonationService);
  private readonly userService = inject(UserService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  readonly activeTab = signal<AdminTab>('resumen');
  readonly campanas = signal<CampaignResponse[]>([]);
  readonly donaciones = signal<DonationResponse[]>([]);
  readonly donationsPage = signal<PagedResponse<DonationResponse> | null>(null);
  readonly donationsPageIndex = signal(0);
  readonly donationsPageSize = signal(10);
  readonly usuarios = signal<UserResponse[]>([]);

  readonly campaignModalOpen = signal(false);
  readonly editingCampaignId = signal<number | null>(null);
  readonly savingCampaign = signal(false);
  readonly campaignModalError = signal('');

  readonly campaignToDelete = signal<CampaignResponse | null>(null);

  readonly totalRecaudadoGlobal = computed(() =>
    this.campanas().reduce((acc, c) => acc + Number(c.amountCollected || 0), 0),
  );

  readonly campanasActivas = computed(
    () => this.campanas().filter((c) => c.status === 'ACTIVE').length,
  );
  readonly totalCampanas = computed(() => this.campanas().length);
  readonly totalDonacionesGlobal = computed(() => this.donationsPage()?.totalElements ?? 0);
  readonly totalUsuariosGlobal = computed(() => this.usuarios().length);
  readonly recentDonationsPreview = computed(() => this.donaciones().slice(0, 6));
  readonly donationsTotalPages = computed(() => this.donationsPage()?.totalPages ?? 0);
  readonly donationsCurrentPage = computed(() => (this.donationsPage()?.page ?? 0) + 1);
  readonly canGoToPreviousDonationsPage = computed(() => this.donationsPageIndex() > 0);
  readonly canGoToNextDonationsPage = computed(
    () => this.donationsPageIndex() + 1 < this.donationsTotalPages(),
  );
  readonly campaignForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    targetAmount: [1000, [Validators.required, Validators.min(100)]],
    status: ['ACTIVE' as CampaignStatus, [Validators.required]],
    category: ['Educación' as CampaignCategory, [Validators.required]],
    imageUrl: ['', [Validators.maxLength(255)]],
    description: ['', [Validators.required, Validators.maxLength(255)]],
    history: ['', [Validators.maxLength(255)]],
  });

  readonly createUserForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    rol: ['USUARIO' as RolNombre, [Validators.required]],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(PASSWORD_MIN_LENGTH),
        Validators.maxLength(PASSWORD_MAX_LENGTH),
        Validators.pattern(PASSWORD_REGEX),
      ],
    ],
  });
  readonly editUserForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],

    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],

    rol: ['USUARIO' as RolNombre, [Validators.required]],
  });
  readonly uploadingImage = signal(false);
  readonly previewUrl = signal<string | null>(null);
  readonly imageUploadError = signal<string | null>(null);

  ngOnInit(): void {
    this.route.fragment.subscribe((frag) => {
      if (
        frag === 'resumen' ||
        frag === 'campanas' ||
        frag === 'donaciones' ||
        frag === 'usuarios'
      ) {
        this.activeTab.set(frag);
      }
    });

    this.loadAllAdminData();
  }

  ngOnDestroy(): void {
    this.clearSelectedCampaignImage();
  }

  loadAllAdminData(): void {
    this.campaignService.getAllForAdmin().subscribe({
      next: (list) => {
        this.campanas.set(list || []);
      },
      error: (err) => {
        this.campanas.set([]);
        this.toast.error(extractApiErrorMessage(err, 'No se pudieron cargar las campañas.'));
      },
    });

    this.loadDonationsPage();

    this.userService.getAll().subscribe({
      next: (list) => {
        this.usuarios.set(list || []);
      },
      error: () => this.usuarios.set([]),
    });
  }

  loadDonationsPage(page: number = this.donationsPageIndex()): void {
    this.donationService.getDonationsPage(page, this.donationsPageSize()).subscribe({
      next: (res) => {
        this.donationsPage.set(res);
        this.donaciones.set(res.content || []);
        this.donationsPageIndex.set(res.page);
        if (res.totalPages > 0 && res.page >= res.totalPages) {
          this.loadDonationsPage(res.totalPages - 1);
        }
      },
      error: () => {
        this.donationsPage.set(null);
        this.donaciones.set([]);
      },
    });
  }

  goToDonationsPage(page: number): void {
    if (page < 0 || page >= this.donationsTotalPages()) {
      return;
    }
    this.loadDonationsPage(page);
  }

  switchAdminTab(tab: AdminTab): void {
    this.activeTab.set(tab);
  }

  donorInitial(name: string | null | undefined): string {
    return name && name.trim() ? name.trim().charAt(0).toUpperCase() : 'D';
  }

  openCampaignModal(): void {
    this.clearSelectedCampaignImage();

    this.editingCampaignId.set(null);
    this.campaignModalError.set('');

    this.campaignForm.reset({
      title: '',
      targetAmount: 25000,
      status: 'ACTIVE',
      category: 'Educación',
      imageUrl: '',
      description: '',
      history: '',
    });

    this.campaignModalOpen.set(true);
  }

  openEditCampaignModal(c: CampaignResponse): void {
    this.clearSelectedCampaignImage();

    this.editingCampaignId.set(c.id);
    this.campaignModalError.set('');

    this.campaignForm.reset({
      title: c.title || '',
      targetAmount: Number(c.targetAmount || 100),
      status: c.status || 'ACTIVE',
      category: (['Educación', 'Salud', 'Vivienda', 'Alimentación'].includes(c.category)
        ? c.category
        : 'Educación') as CampaignCategory,
      imageUrl: c.imageUrl || '',
      description: c.description || '',
      history: c.history || '',
    });

    this.campaignModalOpen.set(true);
  }

  closeCampaignModal(): void {
    if (this.savingCampaign()) return;

    this.campaignModalOpen.set(false);
    this.clearSelectedCampaignImage();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.imageUploadError.set(null);

    const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!validTypes.includes(file.type)) {
      this.imageUploadError.set('Solo JPG, PNG o WEBP');
      input.value = '';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.imageUploadError.set('Máximo 5 MB');
      input.value = '';
      return;
    }

    this.uploadingImage.set(true);
    this.previewUrl.set(URL.createObjectURL(file));

    const id = this.editingCampaignId();
    const request$ =
      id !== null
        ? this.campaignService.uploadCampaignImage(id, file)
        : this.campaignService.uploadImage(file);

    request$.subscribe({
      next: (res) => {
        this.campaignForm.patchValue({ imageUrl: res.url });
        this.uploadingImage.set(false);
        this.previewUrl.set(null);
        this.toast.success('Imagen subida a Cloudinary');
      },
      error: (err) => {
        this.uploadingImage.set(false);
        this.previewUrl.set(null);
        const msg = extractApiErrorMessage(err, 'No se pudo subir la imagen');
        this.imageUploadError.set(msg);
        this.toast.error(msg);
      },
    });
  }

  private clearSelectedCampaignImage(): void {
    const url = this.previewUrl();
    if (url?.startsWith('blob:')) {
      URL.revokeObjectURL(url);
    }

    this.previewUrl.set(null);
    this.imageUploadError.set(null);
  }

  handleSaveCampaign(): void {
    if (this.savingCampaign()) return;

    this.campaignModalError.set('');

    if (this.campaignForm.invalid) {
      this.campaignForm.markAllAsTouched();

      this.campaignModalError.set(
        'Verifica el título, la meta mínima de S/ 100.00 y la descripción.',
      );
      return;
    }

    const raw = this.campaignForm.getRawValue();

    const payload = {
      title: raw.title.trim(),
      targetAmount: Number(raw.targetAmount),
      status: raw.status,
      category: raw.category,
      imageUrl: raw.imageUrl.trim(),
      description: raw.description.trim(),
      history: raw.history.trim() || raw.description.trim(),
    };

    const id = this.editingCampaignId();

    const request$ =
      id !== null ? this.campaignService.update(id, payload) : this.campaignService.create(payload);

    this.savingCampaign.set(true);

    request$.subscribe({
      next: () => {
        this.savingCampaign.set(false);
        this.closeCampaignModal();

        this.toast.success(
          id !== null ? 'Campaña actualizada correctamente.' : 'Campaña creada exitosamente.',
        );

        this.activeTab.set('campanas');
        this.loadAllAdminData();
      },
      error: (err) => {
        this.savingCampaign.set(false);

        this.campaignModalError.set(
          extractApiErrorMessage(err, 'No se pudo subir la imagen o guardar la campaña.'),
        );
      },
    });
  }

  changeCampaignStatus(id: number, event: Event): void {
    const status = (event.target as HTMLSelectElement).value as CampaignStatus;

    this.campaignService.updateStatus(id, status).subscribe({
      next: () => {
        this.toast.success(`Estado de campaña #${id} actualizado a ${status}.`);
        this.loadAllAdminData();
      },
      error: (err) => {
        const msg = extractApiErrorMessage(err, 'No se pudo actualizar el estado de la campaña.');

        this.toast.error(msg);
        this.loadAllAdminData();
      },
    });
  }

  deleteCampaign(c: CampaignResponse): void {
    this.campaignToDelete.set(c);
  }

  cancelDeleteCampaign(): void {
    this.campaignToDelete.set(null);
  }

  confirmDeleteCampaign(): void {
    const c = this.campaignToDelete();

    if (!c) return;

    this.campaignToDelete.set(null);

    this.campaignService.delete(c.id).subscribe({
      next: () => {
        this.toast.success('Campaña eliminada correctamente.');
        this.loadAllAdminData();
      },
      error: (err) => {
        const msg = extractApiErrorMessage(err, 'No se puede eliminar esta campaña.');
        this.toast.error(msg);
      },
    });
  }
  changeDonationStatus(id: number, event: Event): void {
    const status = (event.target as HTMLSelectElement).value as PaymentStatus;

    this.donationService.updateStatus(id, status).subscribe({
      next: () => {
        this.toast.success(`Estado de donación #${id} actualizado a ${status}.`);

        this.loadAllAdminData();
      },
      error: (err) => {
        const msg = extractApiErrorMessage(err, 'Error al actualizar el estado de la donación.');

        this.toast.error(msg);
        this.loadAllAdminData();
      },
    });
  }

  openCreateUserModal(): void {
    this.createUserError.set('');

    this.createUserForm.reset({
      name: '',
      email: '',
      rol: 'USUARIO',
      password: '',
    });

    this.createUserModalOpen.set(true);
  }

  closeCreateUserModal(): void {
    this.createUserModalOpen.set(false);
  }

  handleAdminCreateUser(): void {
    if (this.savingUser()) return;

    this.createUserError.set('');

    if (this.createUserForm.invalid) {
      this.createUserForm.markAllAsTouched();

      this.createUserError.set(
        'Completa correctamente todos los campos. La contraseña requiere entre 8 y 72 caracteres, mayúscula, minúscula y número.',
      );
      return;
    }

    const { name, email, rol, password } = this.createUserForm.getRawValue();

    this.savingUser.set(true);

    this.userService
      .create({
        name: name.trim(),
        email: email.trim(),
        rol,
        password,
      })
      .subscribe({
        next: () => {
          this.savingUser.set(false);
          this.closeCreateUserModal();

          this.toast.success('Usuario creado exitosamente en la base de datos.');

          this.activeTab.set('usuarios');
          this.loadAllAdminData();
        },
        error: (err) => {
          this.savingUser.set(false);

          const msg = extractApiErrorMessage(err, 'No se pudo crear el usuario.');

          this.createUserError.set(msg);
        },
      });
  }

  toggleUserActive(u: UserResponse): void {
    this.userService
      .update(u.id, {
        active: !u.active,
      })
      .subscribe({
        next: () => {
          this.toast.success(`Estado del usuario #${u.id} actualizado.`);

          this.loadAllAdminData();
        },
        error: (err) => {
          const msg = extractApiErrorMessage(err, 'No se pudo cambiar el estado del usuario.');

          this.toast.error(msg);
        },
      });
  }
  openEditUser(u: UserResponse): void {
    if (u.id === this.auth.currentUser()?.id) return;

    this.selectedUser.set(u);
    this.editUserError.set('');

    this.editUserForm.reset({
      name: u.name,
      email: u.email,
      rol: u.rolName === 'ADMINISTRADOR' ? 'ADMINISTRADOR' : 'USUARIO',
    });

    this.editUserModalOpen.set(true);
  }

  closeEditUser(): void {
    if (this.savingEditUser()) return;

    this.editUserModalOpen.set(false);
    this.selectedUser.set(null);
    this.editUserError.set('');
  }

  saveEditUser(): void {
    const user = this.selectedUser();

    if (!user || this.savingEditUser()) return;

    this.editUserError.set('');

    if (this.editUserForm.invalid) {
      this.editUserForm.markAllAsTouched();
      this.editUserError.set('Verifica los datos ingresados.');
      return;
    }

    const { name, email, rol } = this.editUserForm.getRawValue();

    this.savingEditUser.set(true);

    this.userService
      .update(user.id, {
        name: name.trim(),
        email: email.trim(),
        rol,
      })
      .subscribe({
        next: () => {
          this.savingEditUser.set(false);
          this.closeEditUser();

          this.toast.success('Usuario actualizado correctamente.');

          this.loadAllAdminData();
        },

        error: (err) => {
          this.savingEditUser.set(false);

          const msg = extractApiErrorMessage(err, 'No se pudo actualizar el usuario.');

          this.editUserError.set(msg);
          this.toast.error(msg);
        },
      });
  }
  deleteUser(u: UserResponse): void {
    if (u.id === this.auth.currentUser()?.id) return;

    this.userToDelete.set(u);
  }

  cancelDeleteUser(): void {
    this.userToDelete.set(null);
  }

  confirmDeleteUser(): void {
    const user = this.userToDelete();

    if (!user) return;

    this.userToDelete.set(null);

    this.userService.delete(user.id).subscribe({
      next: () => {
        this.toast.success('Usuario eliminado correctamente.');

        this.loadAllAdminData();
      },

      error: (err) => {
        const msg = extractApiErrorMessage(err, 'No se pudo eliminar el usuario.');

        this.toast.error(msg);
      },
    });
  }
}
